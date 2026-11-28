package com.fitotv.data

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

    suspend fun refresh(p: Playlist, channels: List<Channel>): Boolean = withContext(Background) {
        val url = guideUrl(p)
        if (url.isBlank()) return@withContext false
        val wanted = channels.asSequence().filter { it.kind == ContentKind.LIVE && it.tvgId.isNotBlank() }
            .mapTo(HashSet()) { it.tvgId.lowercase() }
        if (wanted.isEmpty()) return@withContext false
        try {
            val map = Http.open(url) { parse(it, wanted) }
            store.saveEpg(p.id, map)
            cache[p.id] = map
            true
        } catch (e: Exception) {
            false
        }
    }

    fun forget(listId: String) {
        cache.remove(listId)
    }

    private fun parse(input: InputStream, wanted: Set<String>): Map<String, List<Programme>> {
        val now = System.currentTimeMillis()
        val from = now - 60 * 60 * 1000L
        val to = now + 30 * 60 * 60 * 1000L
        val out = HashMap<String, MutableList<Programme>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channel: String? = null
        var start = 0L
        var stop = 0L
        var title = ""
        var desc = ""
        var inProgramme = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        val ch = parser.getAttributeValue(null, "channel")?.lowercase()
                        if (ch != null && ch in wanted) {
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            stop = parseTime(parser.getAttributeValue(null, "stop"))
                            if (stop > from && start < to && start > 0) {
                                inProgramme = true
                                channel = ch
                                title = ""
                                desc = ""
                            }
                        }
                    }
                    "title" -> if (inProgramme && title.isEmpty()) title = parser.nextText().trim()
                    "desc" -> if (inProgramme && desc.isEmpty()) desc = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme" && inProgramme) {
                    val ch = channel
                    if (ch != null) {
                        out.getOrPut(ch) { ArrayList() }.add(Programme(start, stop, title.ifEmpty { "Sin título" }, desc.take(400)))
                    }
                    inProgramme = false
                    channel = null
                }
            }
            event = parser.next()
        }
        return out.mapValues { (_, v) -> v.sortedBy { it.start } }
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
        /** Lo que está pasando ahora y lo que sigue. */
        fun nowNext(list: List<Programme>?, now: Long = System.currentTimeMillis()): Pair<Programme?, Programme?> {
            if (list.isNullOrEmpty()) return null to null
            val idx = list.indexOfFirst { it.start <= now && it.stop > now }
            if (idx < 0) return null to list.firstOrNull { it.start > now }
            return list[idx] to list.getOrNull(idx + 1)
        }
    }
}
