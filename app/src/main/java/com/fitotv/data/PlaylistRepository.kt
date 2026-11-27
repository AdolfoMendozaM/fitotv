package com.fitotv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

sealed interface RefreshResult {
    data class Ok(val count: Int, val newLive: Int) : RefreshResult
    data class Failed(val message: String, val hasSavedCopy: Boolean, val savedAt: Long) : RefreshResult
}

/** Descarga, lee y guarda las listas. Si una lista no responde, seguimos con la copia guardada. */
class PlaylistRepository(private val store: Store) {

    private val cache = ConcurrentHashMap<String, List<Channel>>()

    suspend fun channels(listId: String): List<Channel> = withContext(Dispatchers.IO) {
        cache[listId] ?: store.loadChannels(listId)?.also { cache[listId] = it } ?: emptyList()
    }

    fun cached(listId: String): List<Channel>? = cache[listId]

    /** Revisa una lista nueva antes de guardarla. Devuelve los canales o un mensaje claro. */
    suspend fun fetch(p: Playlist): M3uParser.Result = withContext(Dispatchers.IO) {
        try {
            when (p.kind) {
                ListKind.URL -> Http.open(p.url) { M3uParser.parse(it.bufferedReader(), p.id) }
                ListKind.XTREAM -> Http.open(Xtream.m3uUrl(p.server, p.user, p.pass)) { M3uParser.parse(it.bufferedReader(), p.id) }
                ListKind.FILE -> {
                    val f = store.listFile(p.id)
                    if (!f.exists()) throw IOException("No encontramos el archivo de la lista")
                    f.bufferedReader().use { M3uParser.parse(it, p.id) }
                }
            }
        } catch (e: UnknownHostException) {
            throw IOException("No encontramos esa dirección. Revisa que esté bien escrita y que la tele tenga internet")
        } catch (e: SocketTimeoutException) {
            throw IOException("El servidor de la lista tardó demasiado en responder")
        } catch (e: IllegalArgumentException) {
            throw IOException("Esa dirección no parece un enlace válido")
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
            val result = fetch(p)
            if (result.channels.isEmpty()) throw IOException("La lista llegó sin canales")
            val oldIds = old.mapTo(HashSet()) { it.id }
            val newLive = if (old.isEmpty()) 0 else result.channels.count { it.kind == ContentKind.LIVE && it.id !in oldIds }
            commit(p, result)
            RefreshResult.Ok(result.channels.size, newLive)
        } catch (e: Exception) {
            val msg = e.message ?: "No pudimos actualizar la lista"
            store.update { d -> d.copy(playlists = d.playlists.map { if (it.id == p.id) it.copy(lastError = msg) else it }) }
            RefreshResult.Failed(msg, old.isNotEmpty(), p.updatedAt)
        }
    }

    fun forget(listId: String) {
        cache.remove(listId)
        store.deleteListFiles(listId)
    }
}
