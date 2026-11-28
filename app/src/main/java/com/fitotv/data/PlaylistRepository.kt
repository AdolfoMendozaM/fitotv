package com.fitotv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

sealed interface RefreshResult {
    /** [remap]: canales que cambiaron de id (por ejemplo al pasar del M3U a la API de Xtream). */
    data class Ok(val count: Int, val newLive: Int, val remap: Map<String, String> = emptyMap()) : RefreshResult
    data class Failed(val message: String, val hasSavedCopy: Boolean, val savedAt: Long) : RefreshResult
}

/** Descarga, lee y guarda las listas. Si una lista no responde, seguimos con la copia guardada. */
class PlaylistRepository(private val store: Store) {

    private val cache = ConcurrentHashMap<String, List<Channel>>()

    suspend fun channels(listId: String): List<Channel> = withContext(Dispatchers.IO) {
        cache[listId]?.let { return@withContext it }
        val binary = store.hasChannels(listId)
        var list = store.loadChannels(listId) ?: return@withContext emptyList()
        // Copia de una versión anterior: los nombres llegaban sin limpiar; se limpian y se guarda en binario.
        if (!binary) {
            list = cleanLiveNames(list)
            store.saveChannels(listId, list)
        }
        cache[listId] = list
        list
    }

    fun cached(listId: String): List<Channel>? = cache[listId]

    /** Revisa una lista nueva antes de guardarla. Devuelve los canales o un mensaje claro. */
    suspend fun fetch(p: Playlist, background: Boolean = false): M3uParser.Result = withContext(if (background) Background else Dispatchers.IO) {
        try {
            val raw = when (p.kind) {
                ListKind.XTREAM -> viaApi(XtreamApi.Creds(p.server, p.user, p.pass)) {
                    Http.open(Xtream.m3uUrl(p.server, p.user, p.pass)) { M3uParser.parse(it.bufferedReader(), p.id) }
                }
                ListKind.URL -> {
                    val m3u = { Http.open(p.url) { M3uParser.parse(it.bufferedReader(), p.id) } }
                    // Un enlace get.php de Xtream: mejor por la API (series por temporadas y mucho más ligero).
                    val creds = XtreamApi.credsFromUrl(p.url)
                    if (creds != null) viaApi(creds, m3u) else m3u()
                }
                ListKind.FILE -> {
                    val f = store.listFile(p.id)
                    if (!f.exists()) throw IOException("No encontramos el archivo de la lista")
                    f.bufferedReader().use { M3uParser.parse(it, p.id) }
                }
            }
            raw.copy(channels = cleanLiveNames(raw.channels))
        } catch (e: UnknownHostException) {
            throw IOException("No encontramos esa dirección. Revisa que esté bien escrita y que la tele tenga internet")
        } catch (e: SocketTimeoutException) {
            throw IOException("El servidor de la lista tardó demasiado en responder")
        } catch (e: IllegalArgumentException) {
            throw IOException("Esa dirección no parece un enlace válido")
        }
    }

    /** Primero la API de Xtream; si el servidor no la tiene (o falla), el M3U de siempre. */
    private fun viaApi(creds: XtreamApi.Creds, m3u: () -> M3uParser.Result): M3uParser.Result =
        try {
            XtreamApi.load(creds)
        } catch (e: XtreamApi.AccountProblem) {
            throw e
        } catch (e: UnknownHostException) {
            throw e
        } catch (e: Exception) {
            m3u()
        }

    /**
     * Nombres limpios solo en los canales en vivo ("≡P2 - Canal 5" → "Canal 5"). En películas y
     * series no se tocan: ahí "E05" o "(2019)" son parte del nombre. Se guarda ya limpio para no
     * repetir el trabajo cada vez que se abre la app.
     */
    private fun cleanLiveNames(all: List<Channel>): List<Channel> {
        val idx = all.indices.filter { all[it].kind == ContentKind.LIVE }
        if (idx.isEmpty()) return all
        val cleaned = ChannelNames.clean(idx.map { all[it] })
        val out = all.toMutableList()
        idx.forEachIndexed { j, i -> out[i] = cleaned[j] }
        return out
    }

    /** Temporadas de una serie de Xtream (se piden al abrirla y se recuerdan un rato). */
    private val seriesCache = ConcurrentHashMap<String, Pair<Long, XtreamApi.SeriesInfo>>()

    suspend fun seriesInfo(p: Playlist, show: Channel): XtreamApi.SeriesInfo = withContext(Dispatchers.IO) {
        val key = p.id + "|" + show.seriesId
        seriesCache[key]?.let { (at, info) -> if (System.currentTimeMillis() - at < 30 * 60_000L) return@withContext info }
        val creds = if (p.kind == ListKind.XTREAM) XtreamApi.Creds(p.server, p.user, p.pass) else XtreamApi.credsFromUrl(p.url)
            ?: throw IOException("Esta lista no trae las temporadas de la serie")
        try {
            XtreamApi.seriesInfo(creds, show).also { seriesCache[key] = System.currentTimeMillis() to it }
        } catch (e: UnknownHostException) {
            throw IOException("No pudimos conectarnos con tu proveedor. Revisa el internet de la tele")
        } catch (e: SocketTimeoutException) {
            throw IOException("Tu proveedor tardó demasiado en responder. Intenta otra vez")
        } catch (e: XtreamApi.NotXtream) {
            throw IOException("Tu proveedor no mandó las temporadas de esta serie")
        }
    }

    /** Guarda los canales de una lista que ya revisamos. */
    suspend fun commit(p: Playlist, result: M3uParser.Result) = withContext(Dispatchers.IO) {
        store.saveChannels(p.id, result.channels)
        cache[p.id] = result.channels
        val now = System.currentTimeMillis()
        store.update { d ->
            d.copy(playlists = d.playlists.map {
                if (it.id == p.id) it.copy(
                    updatedAt = now,
                    channelCount = result.channels.size,
                    epgUrl = it.epgUrl.ifBlank { result.epgUrl },
                    lastError = null,
                ) else it
            })
        }
    }

    suspend fun refresh(p: Playlist): RefreshResult = withContext(Dispatchers.IO) {
        val old = channels(p.id)
        try {
            val result = fetch(p, background = true)
            if (result.channels.isEmpty()) throw IOException("La lista llegó sin canales")
            val oldIds = old.mapTo(HashSet()) { it.id }
            val remap = remap(old, result.channels)
            val newLive = if (old.isEmpty()) 0 else result.channels.count {
                it.kind == ContentKind.LIVE && it.id !in oldIds && it.id !in remap.values
            }
            commit(p, result)
            RefreshResult.Ok(result.channels.size, newLive, remap)
        } catch (e: Exception) {
            val msg = e.message ?: "No pudimos actualizar la lista"
            store.update { d -> d.copy(playlists = d.playlists.map { if (it.id == p.id) it.copy(lastError = msg) else it }) }
            RefreshResult.Failed(msg, old.isNotEmpty(), p.updatedAt)
        }
    }

    /**
     * Si un canal cambió de id pero es el mismo (mismo número de stream del proveedor), se avisa
     * para que favoritos y "Continuar viendo" lo sigan encontrando.
     */
    private fun remap(old: List<Channel>, new: List<Channel>): Map<String, String> {
        if (old.isEmpty()) return emptyMap()
        val newIds = new.mapTo(HashSet()) { it.id }
        val byKey = HashMap<String, String>()
        new.forEach { c -> streamKey(c)?.let { byKey.putIfAbsent(it, c.id) } }
        val out = HashMap<String, String>()
        old.forEach { o -> if (o.id !in newIds) streamKey(o)?.let { byKey[it] }?.let { out[o.id] = it } }
        return out
    }

    private fun streamKey(c: Channel): String? {
        val last = c.url.substringBefore('?').substringAfterLast('/').substringBefore('.')
        if (last.isEmpty() || !last.all { it.isDigit() }) return null
        return c.kind.name + ":" + last
    }

    fun forget(listId: String) {
        cache.remove(listId)
        store.deleteListFiles(listId)
    }
}
