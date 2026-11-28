package com.fitotv.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Para encontrar un canal en una guía pública por su nombre: "HBO Family SD", "HBO Family HD" y
 * "HBO FAMILY" quedan como "hbo family"; "Warner FHD" y "Warner Channel" como "warner".
 */
object EpgNames {
    /** Palabras que no ayudan a distinguir un canal: calidad, "canal", región de la señal. */
    private val noise = setOf(
        "hd", "fhd", "uhd", "sd", "4k", "8k", "hevc", "h265", "h264", "fullhd", "1080p", "720p", "1080", "720", "480p",
        "channel", "canal", "latino", "latin", "latinoamerica", "latam", "lat", "la", "mexico", "mx", "en",
        "east", "west", "este", "oeste", "feed", "central", "panregional", "tv", "network", "senal",
    )
    private val quality = setOf("hd", "fhd", "uhd", "sd", "4k", "8k", "hevc", "fullhd")
    private val split = Regex("[^a-z0-9+]+")
    private val cache = ConcurrentHashMap<String, String>()

    /** Clave para comparar nombres; vacía si el nombre no sirve para buscar (solo números, por ejemplo). */
    fun key(name: String): String = cache.getOrPut(name) { compute(name) }

    private fun compute(name: String): String {
        val words = Fold.fold(NameText.deobfuscate(name)).replace("&", "").split(split).filter { it.isNotEmpty() }
        var kept = words.filter { it !in noise }
        // "Canal 5" no debe quedar como "5": si solo quedan números, se respetan las palabras.
        if (kept.none { w -> w.any { it.isLetter() } }) kept = words.filter { it !in quality }
        if (kept.none { w -> w.any { it.isLetter() } }) return ""
        return kept.joinToString(" ")
    }

    /** Clave con la que se guarda en la guía lo que viene de una guía pública. */
    fun mapKey(name: String): String {
        val k = key(name)
        return if (k.isEmpty()) "" else "n:$k"
    }
}
