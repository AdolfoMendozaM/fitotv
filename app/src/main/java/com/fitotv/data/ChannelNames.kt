package com.fitotv.data

/**
 * Limpia los nombres que traen las listas para que quepan en pantalla.
 *
 * - Quita adornos al inicio y al final (≡ ★ | • …).
 * - Quita etiquetas del proveedor que se repiten en muchos canales: "P2 - ", "MX: ", "[LAT] ".
 *   Una etiqueta solo se quita si parece código (letras con número, país o va entre corchetes)
 *   y aparece en varios canales; así "TNT - Series" o "PPV 1 - Box" se respetan.
 * - Si al limpiar dos canales distintos quedan con el mismo nombre, se les deja la etiqueta al final
 *   ("Canal 5 · P2") para poder distinguirlos.
 *
 * Los id no cambian (se calcularon con el nombre original), así que recientes y favoritos siguen igual.
 */
object ChannelNames {
    private const val JUNK = "\\s≡☰★☆✪✦●○◉◆◇♦■□▪▫▶►▸»›|┃¦•·~*#=_>:\\-–—"
    private val leadingJunk = Regex("^[$JUNK]+")
    private val trailingJunk = Regex("[$JUNK]+$")
    private val bracketTag = Regex("^(\\[[^\\]]{1,14}]|\\([^)]{1,14}\\)|\\|[^|]{1,14}\\||【[^】]{1,14}】)\\s*")
    private val codeTag = Regex("^([\\p{L}\\p{N}]{1,6})\\s*[-–—:|•»]\\s*(?=\\S)")
    private val codeLike = Regex("^[A-Za-z]{1,3}\\d{1,3}$")
    private val spaces = Regex("\\s{2,}")
    private val countries = setOf(
        "MX", "MEX", "US", "USA", "ES", "ESP", "SPA", "AR", "ARG", "CO", "COL", "PE", "PER", "CL", "CHI",
        "EC", "VE", "UY", "PY", "BO", "CR", "GT", "HN", "SV", "NI", "PA", "DO", "RD", "PR", "CU", "BR",
        "PT", "UK", "GB", "CA", "FR", "DE", "IT", "LA", "LAT", "LATAM", "LATINO", "LATIN", "EN", "ENG",
    )

    /** Devuelve los canales con el nombre limpio, en el mismo orden. */
    fun clean(channels: List<Channel>): List<Channel> {
        if (channels.isEmpty()) return channels
        // 1) Cuántas veces aparece cada etiqueta en la lista.
        val counts = HashMap<String, Int>()
        channels.forEach { ch -> tags(ch.name).forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        val minRepeats = if (channels.size < 10) 2 else 3
        val removable = counts.filterValues { it >= minRepeats }.keys

        // 2) Limpieza; guardamos qué etiquetas se quitaron por si hay que devolverlas.
        val cleaned = channels.map { ch -> strip(ch.name, removable) }

        // 3) Choques: dos canales distintos con el mismo nombre limpio.
        val byName = cleaned.indices.groupBy { cleaned[it].first.lowercase() }
        return channels.mapIndexed { i, ch ->
            val (name, removed) = cleaned[i]
            val clash = (byName[name.lowercase()]?.size ?: 0) > 1 &&
                byName[name.lowercase()]!!.any { cleaned[it].second != removed }
            val finalName = if (clash && removed.isNotEmpty()) "$name · ${removed.last()}" else name
            if (finalName == ch.name) ch else ch.copy(name = finalName)
        }
    }

    /** Etiquetas que podrían quitarse del inicio de un nombre (normalizadas). */
    private fun tags(raw: String): List<String> {
        val out = ArrayList<String>(2)
        var s = raw.replace(leadingJunk, "")
        repeat(3) {
            val tag = nextTag(s) ?: return out
            out += tag.first
            s = s.substring(tag.second).replace(leadingJunk, "")
        }
        return out
    }

    /** La etiqueta al inicio de [s] si parece código del proveedor: (clave, largo a quitar). */
    private fun nextTag(s: String): Pair<String, Int>? {
        bracketTag.find(s)?.let { m ->
            val inner = m.value.trim().trim('[', ']', '(', ')', '|', '【', '】').trim()
            if (inner.isNotEmpty()) return inner.uppercase() to m.value.length
        }
        codeTag.find(s)?.let { m ->
            val token = m.groupValues[1]
            if (codeLike.matches(token) || token.uppercase() in countries) return token.uppercase() to m.value.length
        }
        return null
    }

    private fun strip(raw: String, removable: Set<String>): Pair<String, List<String>> {
        val removed = ArrayList<String>(2)
        var s = raw.replace(leadingJunk, "")
        repeat(3) {
            val tag = nextTag(s) ?: return@repeat
            if (tag.first !in removable) return@repeat
            val rest = s.substring(tag.second).replace(leadingJunk, "")
            if (rest.isBlank()) return@repeat
            removed += tag.first
            s = rest
        }
        s = s.replace(trailingJunk, "").replace(spaces, " ").trim()
        return (s.ifBlank { raw.trim() }) to removed
    }
}
