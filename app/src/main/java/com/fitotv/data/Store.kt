package com.fitotv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Todo lo que FitoTV guarda en la tele: perfiles, listas, recientes y ajustes en un
 * archivo, y una copia de los canales y de la guía de cada lista en archivos aparte.
 */
@OptIn(ExperimentalSerializationApi::class)
class Store(private val dir: File) {

    private companion object {
        const val MAGIC = 0x46540003 // "FT" + versión 3 del formato (con fecha de subida)
        const val MAGIC_V2 = 0x46540002
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateFile = File(dir, "fitotv.json")
    private val listsDir = File(dir, "lists").apply { mkdirs() }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private fun load(): AppData = try {
        if (stateFile.exists()) json.decodeFromString(AppData.serializer(), stateFile.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        _data.value = next
        writeAtomic(stateFile, json.encodeToString(AppData.serializer(), next))
    }

    // ----- Copia guardada de cada lista -----

    private val channelsSerializer = ListSerializer(Channel.serializer())

    /**
     * Los canales se guardan en binario y se leen por partes: con listas de cientos de miles
     * de canales, armar un texto JSON gigante llenaba la memoria de los Fire TV y cerraba la app.
     */
    fun saveChannels(listId: String, channels: List<Channel>) {
        val target = File(listsDir, "$listId.channels.bin")
        val tmp = File(listsDir, "$listId.channels.bin.tmp")
        DataOutputStream(BufferedOutputStream(FileOutputStream(tmp), 1 shl 16)).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(channels.size)
            for (c in channels) {
                out.str(c.id); out.writeInt(c.num); out.str(c.name); out.str(c.url); out.str(c.logo)
                out.str(c.group); out.str(c.tvgId); out.writeByte(c.kind.ordinal); out.str(c.userAgent)
                out.str(c.referrer); out.str(c.seriesId); out.writeShort(c.season); out.writeShort(c.episode)
                out.writeLong(c.added)
            }
        }
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
        File(listsDir, "$listId.channels.json").delete()
    }

    fun hasChannels(listId: String): Boolean = File(listsDir, "$listId.channels.bin").exists()

    fun loadChannels(listId: String): List<Channel>? {
        val bin = File(listsDir, "$listId.channels.bin")
        if (bin.exists()) {
            try {
                DataInputStream(BufferedInputStream(FileInputStream(bin), 1 shl 16)).use { inp ->
                    val version = inp.readInt()
                    if (version != MAGIC && version != MAGIC_V2) return null
                    val withDate = version == MAGIC
                    val n = inp.readInt()
                    val out = ArrayList<Channel>(n)
                    // Los grupos se repiten miles de veces: una sola copia de cada texto.
                    val pool = HashMap<String, String>()
                    fun shared(s: String) = if (s.isEmpty()) "" else pool.getOrPut(s) { s }
                    repeat(n) {
                        out.add(
                            Channel(
                                id = inp.str(), num = inp.readInt(), name = inp.str(), url = inp.str(), logo = inp.str(),
                                group = shared(inp.str()), tvgId = inp.str(), kind = ContentKind.entries[inp.readByte().toInt()],
                                userAgent = shared(inp.str()), referrer = shared(inp.str()), seriesId = shared(inp.str()),
                                season = inp.readShort().toInt(), episode = inp.readShort().toInt(),
                                added = if (withDate) inp.readLong() else 0,
                            )
                        )
                    }
                    return out
                }
            } catch (e: Exception) {
                return null
            }
        }
        // Copia de versiones anteriores (JSON). Si es enorme no se intenta: se vuelve a bajar la lista.
        val old = File(listsDir, "$listId.channels.json")
        if (!old.exists() || old.length() > 40L * 1024 * 1024) return null
        return try {
            old.inputStream().buffered().use { json.decodeFromStream(channelsSerializer, it) }
        } catch (e: Exception) {
            null
        }
    }

    /** La guía también va en binario: con miles de canales el JSON pesaba decenas de megas. */
    fun saveEpg(listId: String, epg: Map<String, List<Programme>>) {
        val target = File(listsDir, "$listId.epg.bin")
        val tmp = File(listsDir, "$listId.epg.bin.tmp")
        DataOutputStream(BufferedOutputStream(FileOutputStream(tmp), 1 shl 16)).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(epg.size)
            for ((key, progs) in epg) {
                out.str(key)
                out.writeInt(progs.size)
                for (p in progs) {
                    out.writeLong(p.start); out.writeLong(p.stop); out.str(p.title); out.str(p.desc)
                }
            }
        }
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
        File(listsDir, "$listId.epg.json").delete()
    }

    fun epgSavedAt(listId: String): Long = File(listsDir, "$listId.epg.bin").lastModified()

    fun loadEpg(listId: String): Map<String, List<Programme>>? {
        val f = File(listsDir, "$listId.epg.bin")
        if (!f.exists()) return null
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(f), 1 shl 16)).use { inp ->
                val version = inp.readInt()
                if (version != MAGIC && version != MAGIC_V2) return null
                val n = inp.readInt()
                val out = HashMap<String, List<Programme>>(n * 2)
                repeat(n) {
                    val key = inp.str()
                    val m = inp.readInt()
                    val list = ArrayList<Programme>(m)
                    repeat(m) { list.add(Programme(inp.readLong(), inp.readLong(), inp.str(), inp.str())) }
                    out[key] = list
                }
                out
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Archivo .m3u que llegó desde el celular. */
    fun listFile(listId: String): File = File(listsDir, "$listId.m3u")

    fun deleteListFiles(listId: String) {
        listOf("$listId.channels.json", "$listId.channels.bin", "$listId.epg.json", "$listId.epg.bin", "$listId.m3u").forEach { File(listsDir, it).delete() }
    }

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}

/** Texto con largo de 4 bytes (writeUTF solo aguanta 64 KB y algunos enlaces traen tokens largos). */
private fun DataOutputStream.str(s: String) {
    val b = s.toByteArray(Charsets.UTF_8)
    writeInt(b.size)
    write(b)
}

private fun DataInputStream.str(): String {
    val n = readInt()
    if (n == 0) return ""
    val b = ByteArray(n)
    readFully(b)
    return String(b, Charsets.UTF_8)
}
