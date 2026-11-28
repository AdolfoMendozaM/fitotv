package com.fitotv.data

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/** Guía de programación (XMLTV). Solo guardamos lo que va de una hora atrás a 30 horas adelante. */
class EpgRepository(private val store: Store) {

    private val cache = ConcurrentHashMap<String, Map<String, List<Programme>>>()

    suspend fun load(listId: String): Map<String, List<Programme>> = withContext(Dispatchers.IO) {
        cache[listId] ?: store.loadEpg(listId)?.also { cache[listId] = it } ?: emptyMap()
    }

    fun cached(listId: String): Map<String, List<Programme>> = cache[listId] ?: emptyMap()

    fun savedAt(listId: String): Long = store.epgSavedAt(listId)

    fun guideUrl(p: Playlist): String = when {
        p.kind == ListKind.XTREAM -> Xtream.epgUrl(p.server, p.user, p.pass)
        else -> p.epgUrl
    }

    /**
     * Baja la guía: primero la del proveedor y, para los canales que quedaron sin guía, la guía
     * pública en español buscando el canal por su nombre. Con [providerGuide] en false (listas
     * enormes que piden la guía canal por canal) solo se usa la pública.
     */
    suspend fun refresh(p: Playlist, channels: List<Channel>, providerGuide: Boolean = true): Boolean = withContext(Background) {
        val live = channels.filter { it.kind == ContentKind.LIVE }
        if (live.isEmpty()) return@withContext false
        val map = HashMap<String, List<Programme>>()
        var ok = false
        val url = if (providerGuide) guideUrl(p) else ""
        val wanted = live.asSequence().filter { it.tvgId.isNotBlank() }.mapTo(HashSet()) { it.tvgId.lowercase() }
        if (url.isNotBlank() && wanted.isNotEmpty()) {
            try {
                map.putAll(Http.open(url) { parseProvider(it, wanted) })
                ok = true
            } catch (e: Exception) {
                // seguimos con la guía pública
            }
        }
        val missing = HashSet<String>()
        for (c in live) {
            if (c.tvgId.isNotBlank() && !map[c.tvgId.lowercase()].isNullOrEmpty()) continue
            EpgNames.key(c.name).takeIf { it.isNotEmpty() }?.let { missing.add(it) }
        }
        Log.i("FitoEPG", "guía del proveedor: ${map.size} canales; sin guía: ${missing.size}")
        if (missing.isNotEmpty()) {
            try {
                val t0 = System.currentTimeMillis()
                val pub = Http.open(PUBLIC_GUIDE) { parsePublic(it, missing) }
                map.putAll(pub)
                ok = true
                Log.i("FitoEPG", "guía pública: ${pub.size} de ${missing.size} canales en ${System.currentTimeMillis() - t0} ms")
            } catch (e: Exception) {
                Log.w("FitoEPG", "guía pública no disponible: ${e.message}")
            }
        }
        if (!ok) return@withContext false
        store.saveEpg(p.id, map)
        cache[p.id] = map
        true
    }

    fun forget(listId: String) {
        cache.remove(listId)
    }

    private fun parseProvider(input: InputStream, wanted: Set<String>): Map<String, List<Programme>> {
        val out = HashMap<String, MutableList<Programme>>()
        readXmltv(input, onName = { _, _ -> }) { ch -> ch.lowercase().takeIf { it in wanted } }
            .forEach { (k, v) -> out.getOrPut(k) { ArrayList() }.addAll(v) }
        return out.mapValues { (_, v) -> v.sortedBy { it.start } }
    }

    /**
     * Guía pública: cada canal de la guía se busca por sus nombres. Si varios canales de la guía
     * tienen el mismo nombre (la misma señal en distintos sistemas de cable), se queda el que
     * trae más programas.
     */
    private fun parsePublic(input: InputStream, wanted: Set<String>): Map<String, List<Programme>> {
        val idKey = HashMap<String, String>()
        val byId = readXmltv(
            input,
            onName = { id, name ->
                if (id !in idKey && !name.all { it.isDigit() || it.isWhitespace() }) {
                    val k1 = EpgNames.key(name)
                    val k2 = EpgNames.key(name.replace(leadingNumber, ""))
                    when {
                        k1.isNotEmpty() && k1 in wanted -> idKey[id] = k1
                        k2.isNotEmpty() && k2 in wanted -> idKey[id] = k2
                    }
                }
            },
        ) { ch -> ch.takeIf { it in idKey } }
        val best = HashMap<String, String>()
        for ((id, key) in idKey) {
            val n = byId[id]?.size ?: 0
            val cur = best[key]
            if (n > 0 && (cur == null || n > (byId[cur]?.size ?: 0))) best[key] = id
        }
        val out = HashMap<String, List<Programme>>()
        best.forEach { (key, id) -> byId[id]?.let { out["n:$key"] = it.sortedBy { p -> p.start } } }
        return out
    }

    private val leadingNumber = Regex("^\\d+\\s+")

    /**
     * Lee un XMLTV de principio a fin sin cargarlo completo. [onName] recibe los nombres de cada
     * canal; [bucket] dice en qué lista guardar los programas de un canal (null: no interesa).
     */
    private fun readXmltv(
        input: InputStream,
        onName: (String, String) -> Unit,
        bucket: (String) -> String?,
    ): Map<String, MutableList<Programme>> {
        val now = System.currentTimeMillis()
        val from = now - 60 * 60 * 1000L
        val to = now + 30 * 60 * 60 * 1000L
        val out = HashMap<String, MutableList<Programme>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelId: String? = null
        var key: String? = null
        var start = 0L
        var stop = 0L
        var title = ""
        var desc = ""
        var inProgramme = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "channel" -> channelId = parser.getAttributeValue(null, "id")
                    "display-name" -> {
                        val id = channelId
                        if (id != null) onName(id, parser.nextText().trim())
                    }
                    "programme" -> {
                        channelId = null
                        val ch = parser.getAttributeValue(null, "channel")
                        val k = ch?.let(bucket)
                        if (k != null) {
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            stop = parseTime(parser.getAttributeValue(null, "stop"))
                            if (stop > from && start < to && start > 0) {
                                inProgramme = true
                                key = k
                                title = ""
                                desc = ""
                            }
                        }
                    }
                    "title" -> if (inProgramme && title.isEmpty()) title = parser.nextText().trim()
                    "desc" -> if (inProgramme && desc.isEmpty()) desc = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "channel" -> channelId = null
                    "programme" -> if (inProgramme) {
                        val k = key
                        if (k != null) {
                            out.getOrPut(k) { ArrayList() }.add(Programme(start, stop, title.ifEmpty { "Sin título" }, desc.take(400)))
                        }
                        inProgramme = false
                        key = null
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    private val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmssZ", "yyyyMMddHHmmss")

    private fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0
        val s = raw.trim()
        for (f in formats) {
            try {
                val fmt = SimpleDateFormat(f, Locale.US)
                if (!f.contains('Z')) fmt.timeZone = TimeZone.getTimeZone("UTC")
                val t = fmt.parse(s)?.time
                if (t != null) return t
            } catch (e: Exception) {
                // probamos el siguiente formato
            }
        }
        return 0
    }

    companion object {
        /**
         * Guía pública en español con los canales de cable de México y Latinoamérica (HBO, Warner,
         * TNT, Star, A&E, ESPN, canales abiertos…), de EPGTalk. Se usa para los canales cuya lista no
         * trae guía, buscándolos por su nombre.
         */
        const val PUBLIC_GUIDE = "https://raw.githubusercontent.com/acidjesuz/EPGTalk/master/Latino_guide.xml.gz"

        /** Los programas de un canal: los de su lista y, si no trae, los de la guía pública por nombre. */
        fun programmesOf(epg: Map<String, List<Programme>>, ch: Channel): List<Programme>? {
            if (ch.tvgId.isNotBlank()) epg[ch.tvgId.lowercase()]?.takeIf { it.isNotEmpty() }?.let { return it }
            val k = EpgNames.mapKey(ch.name)
            return if (k.isEmpty()) null else epg[k]
        }

        /** Lo que está pasando ahora y lo que sigue. */
        fun nowNext(list: List<Programme>?, now: Long = System.currentTimeMillis()): Pair<Programme?, Programme?> {
            if (list.isNullOrEmpty()) return null to null
            val idx = list.indexOfFirst { it.start <= now && it.stop > now }
            if (idx < 0) return null to list.firstOrNull { it.start > now }
            return list[idx] to list.getOrNull(idx + 1)
        }
    }
}
