package com.fitotv.data

/**
 * Limpia los nombres que traen las listas para que quepan en pantalla.
 *
 * - Quita adornos al inicio y al final: emojis (🟢 ⭐), símbolos (≡ ★ | •) y espacios.
 * - Quita etiquetas del proveedor que se repiten en muchos canales: "P2 - ", "MX: ", "[LAT] ".
 *   Una etiqueta solo se quita si parece código (letras con número, país o va entre corchetes)
 *   y aparece en varios canales; así "TNT - Series" o "PPV 1 - Box" se respetan.
 * - Quita al final la resolución entre paréntesis "(720p)" y notas entre corchetes que se repiten
 *   en la lista ("[Not 24/7]", "[Geo-blocked]").
 * - También quita códigos del proveedor repetidos al final: "Canal 5 P2", "Canal 5 (P2)", "Canal 5 - P2".
 * - Si al limpiar dos canales quedan con el mismo nombre, solo se les agrega algo que sí diga algo
 *   ("ESPN · MX", "Canal 5 · 720p"); los códigos del proveedor (P1, P2) nunca se muestran.
 *
 * Los id no cambian (se calcularon con el nombre original), así que recientes y favoritos siguen igual.
 */
object ChannelNames {
    // Al inicio: todo lo que no sea letra, número o el comienzo de una etiqueta [MX] (MX) 【MX】.
    private val leadingJunk = Regex("^[^\\p{L}\\p{N}\\[(【]+")
    // Al final: adornos y emojis, pero se respetan "Disney+", "¡Hola!" y cierres de paréntesis.
    private val trailingJunk = Regex("[^\\p{L}\\p{N})\\]】+!?']+$")
    private val bracketTag = Regex("^(\\[[^\\]]{1,14}]|\\([^)]{1,14}\\)|\\|[^|]{1,14}\\||【[^】]{1,14}】)\\s*")
    // Clave del proveedor y un separador: cualquier símbolo ("P2 - ", "P2 ➤ ", "P2 −", "P2: ") o solo espacio.
    private val codeTag = Regex("^([\\p{L}\\p{N}]{1,6})(\\s*[^\\p{L}\\p{N}\\s\\[(【]{1,3}\\s*|\\s+)(?=\\S)")
    // Clave al final: "Canal 5 P2", "Canal 5 (P2)", "Canal 5 [P2]", "Canal 5 - P2".
    private val trailingCode = Regex("(?<=\\S)\\s*(?:[-–—|•·:/]\\s*|\\s)[(\\[]?([A-Za-z]{1,3}\\d{1,3})[)\\]]?\\s*$")
    private val codeLike = Regex("^[A-Za-z]{1,3}\\d{1,3}$")
    // Espacios raros (sin corte, finos, de ancho cero): se vuelven espacio normal antes de limpiar.
    // (Android no acepta la bandera (?U), así que se normaliza el texto en lugar de la expresión.)
    private val oddSpaces = Regex("[\\u00A0\\u1680\\u2000-\\u200B\\u202F\\u205F\\u3000\\uFEFF]")
    private val spaces = Regex("\\s{2,}")
    private val trailingRes = Regex("\\s*\\((\\d{3,4}[pPiI]|4[kK]|UHD|FHD|HD|SD)\\)\\s*$")
    private val trailingNote = Regex("\\s*\\[([^\\]]{1,24})]\\s*$")
    private val countries = setOf(
        "MX", "MEX", "US", "USA", "ES", "ESP", "SPA", "AR", "ARG", "CO", "COL", "PE", "PER", "CL", "CHI",
        "EC", "VE", "UY", "PY", "BO", "CR", "GT", "HN", "SV", "NI", "PA", "DO", "RD", "PR", "CU", "BR",
        "PT", "UK", "GB", "CA", "FR", "DE", "IT", "LA", "LAT", "LATAM", "LATINO", "LATIN", "EN", "ENG",
    )

    /** Devuelve los canales con el nombre limpio, en el mismo orden. */
    fun clean(channels: List<Channel>): List<Channel> {
        if (channels.isEmpty()) return channels
        val minRepeats = if (channels.size < 10) 2 else 3
        // 0) Letras disfrazadas ("F@MILY", "C1NEMAX") y la categoría del proveedor al inicio
        //    ("CINE | HBO", "PERU | Willax") cuando se repite y es la misma del grupo.
        val plain = channels.map { NameText.deobfuscate(it.name.replace(oddSpaces, " ")) }
        val prefixes = plain.map { categoryPrefix(it) }
        val prefixCounts = HashMap<String, Int>()
        prefixes.forEach { p -> if (p != null) prefixCounts[p.first] = (prefixCounts[p.first] ?: 0) + 1 }
        val names = plain.mapIndexed { i, n ->
            val p = prefixes[i] ?: return@mapIndexed n
            val count = prefixCounts[p.first] ?: 0
            val group = Fold.fold(NameText.deobfuscate(channels[i].group))
            val related = group.isNotBlank() && (group.contains(p.first) || p.first.contains(group))
            val rest = n.substring(p.second).trim()
            if (rest.isNotEmpty() && count >= minRepeats && (related || count >= 8)) rest else n
        }
        // 1) Cuántas veces aparece cada etiqueta en la lista.
        val counts = HashMap<String, Int>()
        val notes = HashMap<String, Int>()
        val tails = HashMap<String, Int>()
        names.forEach { name ->
            tags(name).forEach { counts[it] = (counts[it] ?: 0) + 1 }
            trailingNote.find(name)?.let { m -> val k = m.groupValues[1].trim().uppercase(); notes[k] = (notes[k] ?: 0) + 1 }
            trailingCode.find(name.replace(trailingJunk, ""))?.let { m ->
                val k = m.groupValues[1].uppercase(); tails[k] = (tails[k] ?: 0) + 1
            }
        }
        val removable = counts.filterValues { it >= minRepeats }.keys
        val removableNotes = notes.filterValues { it >= minRepeats }.keys
        val removableTails = tails.filterValues { it >= minRepeats }.keys

        // 2) Limpieza; guardamos qué etiquetas se quitaron por si hay que devolverlas.
        val cleaned = names.map { strip(it, removable, removableNotes, removableTails) }

        // 3) Choques: dos canales distintos con el mismo nombre limpio.
        val byName = cleaned.indices.groupBy { cleaned[it].first.lowercase() }
        return channels.mapIndexed { i, ch ->
            val (name, removed) = cleaned[i]
            val clash = (byName[name.lowercase()]?.size ?: 0) > 1 &&
                byName[name.lowercase()]!!.any { cleaned[it].second != removed }
            // Solo se agrega lo que le dice algo a la gente (país, resolución), nunca "P2".
            val hint = removed.lastOrNull { !codeLike.matches(it) }
            val pretty = NameText.prettyCase(name)
            val finalName = if (clash && hint != null) "$pretty · $hint" else pretty
            if (finalName == ch.name) ch else ch.copy(name = finalName)
        }
    }

    // "CINE | HBO", "PERU │ Willax", "DEPORTES » ESPN": la categoría antes de una barra.
    private val categoryTag = Regex("^\\s*([^|│┃»]{2,24}?)\\s*[|│┃»]+\\s*(?=\\S)")

    /** La categoría al inicio (normalizada) y cuánto mide, si la hay. */
    private fun categoryPrefix(name: String): Pair<String, Int>? {
        val m = categoryTag.find(name) ?: return null
        val key = Fold.fold(m.groupValues[1].trim())
        if (key.none { it.isLetter() }) return null
        return key to m.value.length
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
            val onlySpace = m.groupValues[2].isBlank()
            // Con solo un espacio ("P2 Canal 5") únicamente las claves tipo P2; los países necesitan símbolo.
            if (codeLike.matches(token) || (!onlySpace && token.uppercase() in countries)) return token.uppercase() to m.value.length
        }
        return null
    }

    private fun strip(
        raw: String,
        removable: Set<String>,
        removableNotes: Set<String>,
        removableTails: Set<String>,
    ): Pair<String, List<String>> {
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
        // Al final: notas repetidas "[Not 24/7]" y resolución "(720p)", en cualquier orden.
        repeat(3) {
            val note = trailingNote.find(s)
            if (note != null && note.groupValues[1].trim().uppercase() in removableNotes && s.length > note.value.length) {
                s = s.substring(0, note.range.first)
                return@repeat
            }
            val res = trailingRes.find(s)
            if (res != null && s.length > res.value.length) {
                removed += res.groupValues[1]
                s = s.substring(0, res.range.first)
                return@repeat
            }
            val t = s.replace(trailingJunk, "")
            val tail = trailingCode.find(t)
            if (tail != null && tail.groupValues[1].uppercase() in removableTails && t.length > tail.value.length) {
                removed += tail.groupValues[1].uppercase()
                s = t.substring(0, tail.range.first)
            }
        }
        s = s.replace(trailingJunk, "").replace(spaces, " ").trim()
        return (s.ifBlank { raw.trim() }) to removed
    }
}
