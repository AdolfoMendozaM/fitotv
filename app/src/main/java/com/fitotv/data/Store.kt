package com.fitotv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Todo lo que FitoTV guarda en la tele: perfiles, listas, recientes y ajustes en un
 * archivo, y una copia de los canales y de la guía de cada lista en archivos aparte.
 */
class Store(private val dir: File) {

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
    private val epgSerializer = MapSerializer(String.serializer(), ListSerializer(Programme.serializer()))

    fun saveChannels(listId: String, channels: List<Channel>) {
        writeAtomic(File(listsDir, "$listId.channels.json"), json.encodeToString(channelsSerializer, channels))
    }

    fun loadChannels(listId: String): List<Channel>? = try {
        val f = File(listsDir, "$listId.channels.json")
        if (f.exists()) json.decodeFromString(channelsSerializer, f.readText()) else null
    } catch (e: Exception) {
        null
    }

    fun saveEpg(listId: String, epg: Map<String, List<Programme>>) {
        writeAtomic(File(listsDir, "$listId.epg.json"), json.encodeToString(epgSerializer, epg))
    }

    fun loadEpg(listId: String): Map<String, List<Programme>>? = try {
        val f = File(listsDir, "$listId.epg.json")
        if (f.exists()) json.decodeFromString(epgSerializer, f.readText()) else null
    } catch (e: Exception) {
        null
    }

    /** Archivo .m3u que llegó desde el celular. */
    fun listFile(listId: String): File = File(listsDir, "$listId.m3u")

    fun deleteListFiles(listId: String) {
        listOf("$listId.channels.json", "$listId.epg.json", "$listId.m3u").forEach { File(listsDir, it).delete() }
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
