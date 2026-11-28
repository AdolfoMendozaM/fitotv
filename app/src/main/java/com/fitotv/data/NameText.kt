package com.fitotv.data

/**
 * Arreglos de texto para nombres de canales y grupos que llegan "disfrazados" o gritando.
 *
 * - Algunos proveedores cambian letras para que no los encuentren: "F@MILY", "C1NEMAX", "HBÖ",
 *   "DƏPORTES". Aquí vuelven a ser "FAMILY", "CINEMAX", "HBO", "DEPORTES".
 * - Los asteriscos y estrellas de adorno ("*HBO*", "⭐CINE PREMIUM ⭐") se quitan.
 * - Los nombres en puras mayúsculas pasan a mayúsculas y minúsculas ("STAR CHANNEL HD" →
 *   "Star Channel HD"), respetando siglas como HBO, TNT, ESPN o A&E.
 */
object NameText {
    private val decor = Regex("[*★☆✦✧✪✯✰⭐🌟✨⚡🔥❤♥•●◆◇■□▪▫►▶◀◄➤➜→⇒✓✔✅🔴🟢🟡🔵⚽🏀🎬🎥📺🍿🎞🎦]+")
    private val spaces = Regex("\\s{2,}")

    /** Letras cambiadas por símbolos o números entre otras letras, y letras con adornos raros. */
    fun deobfuscate(s: String): String {
        val src = s.replace(decor, " ")
        val sb = StringBuilder(src.length)
        for (i in src.indices) {
            val c = src[i]
            val prev = if (i > 0) src[i - 1] else ' '
            val next = if (i + 1 < src.length) src[i + 1] else ' '
            val between = prev.isLetter() && next.isLetter()
            val lower = prev.isLowerCase() && next.isLowerCase()
            val mapped = when {
                between && c == '@' -> if (lower) 'a' else 'A'
                between && c == '$' -> if (lower) 's' else 'S'
                between && c == '0' -> if (lower) 'o' else 'O'
                between && c == '1' -> if (lower) 'i' else 'I'
                c == 'Ö' || c == 'Ő' -> 'O'
                c == 'ö' || c == 'ő' -> 'o'
                c == 'Ə' || c == 'Ǝ' -> 'E'
                c == 'ə' || c == 'ǝ' -> 'e'
                c == 'Ä' -> 'A'
                c == 'ä' -> 'a'
                c == 'Ë' -> 'E'
                c == 'ë' -> 'e'
                c == 'Ï' -> 'I'
                c == 'ï' -> 'i'
                else -> c
            }
            sb.append(mapped)
        }
        return sb.toString().replace(spaces, " ").trim()
    }

    /** Siglas que se quedan en mayúsculas aunque sean largas. */
    private val acronyms = setOf(
        "ESPN", "ESPN2", "ESPN3", "ESPN4", "HGTV", "CNBC", "MSNBC", "TUDN", "TNT", "HBO", "AMC", "CNN", "MTV", "FOX",
        "TBS", "TCM", "USA", "NBC", "ABC", "CBS", "AXN", "BBC", "NHK", "TVE", "TVN", "NFL", "NBA", "MLB", "NHL",
        "UFC", "WWE", "PPV", "FHD", "UHD", "HEVC", "H265", "H264", "TYC", "TLC", "VH1", "FXM", "DW", "RT", "TV",
        "HD", "SD", "4K", "8K", "MX", "US", "EEUU", "UK", "PE", "CL", "AR", "CO", "EC", "VE", "UY", "PY",
        "BO", "CR", "GT", "HN", "SV", "NI", "PA", "PR", "BR", "DAZN", "NTN24", "RCN", "TV5", "CDF",
        "GOL", "TNT", "VIX", "XXX", "TVC", "ADN", "MVS", "IPN", "UNAM", "TVP", "OTV", "C5N", "TN", "FIFA", "NASA",
    )

    /** Palabras de tres letras que sí son palabras (no siglas). */
    private val shortWords = setOf(
        "SUB", "MAS", "SUR", "SOL", "MAR", "PAZ", "LUZ", "RED", "DIA", "MES", "AÑO", "LAS", "LOS", "DEL", "CON",
        "POR", "SIN", "UNO", "DOS", "MIX", "BOX", "GOLF", "TOP", "HOT", "PRO", "ONE", "TWO", "THE",
        "AND", "BAR", "FAN", "MUY", "VER", "OLA", "OJO", "OYE",
    )

    /** Conectores que van en minúscula a mitad del nombre ("Liga de Campeones"). */
    private val connectors = setOf("de", "del", "la", "las", "el", "los", "y", "e", "en", "con", "por", "para", "sin", "a", "al", "o", "u", "of", "the", "and")

    private val vowels = "AEIOUÁÉÍÓÚÜ"

    /** "STAR CHANNEL HD" → "Star Channel HD". Si el nombre ya trae minúsculas no se toca. */
    fun prettyCase(s: String): String {
        if (s.none { it.isLetter() } || s.any { it.isLowerCase() }) return s
        val words = s.split(' ')
        return words.mapIndexed { i, w -> word(w, i == 0) }.joinToString(" ")
    }

    private fun word(w: String, first: Boolean): String {
        if (w.isEmpty()) return w
        // Con símbolos o números ("A&E", "A3S", "HD2", "E!") se queda como viene.
        if (!w.all { it.isLetter() }) return w
        val up = w.uppercase()
        if (up in acronyms) return up
        val low = w.lowercase()
        if (!first && low in connectors) return low
        if (w.length <= 2) return if (first) title(w) else up
        if (w.length == 3 && up !in shortWords && w.count { it.uppercaseChar() in vowels } <= 1) return up
        return title(w)
    }

    private fun title(w: String) = w.lowercase().replaceFirstChar { it.uppercaseChar() }

    /**
     * Nombre de un grupo o categoría sin emojis ni estrellas: "⭐CINE PREMIUM ⭐" → "Cine Premium",
     * "✨ COPA LIBERTADORES 2026 ✨" → "Copa Libertadores 2026", "DƏPORTES PE" → "Deportes PE".
     */
    fun cleanGroup(raw: String): String {
        val t = deobfuscate(raw)
        val sb = StringBuilder(t.length)
        var i = 0
        while (i < t.length) {
            val cp = t.codePointAt(i)
            val n = Character.charCount(cp)
            val keep = Character.isLetterOrDigit(cp) || cp == ' '.code || "&+-'.,/()!¡¿?#:|".indexOf(cp.toChar()) >= 0 && n == 1
            sb.append(if (keep) String(Character.toChars(cp)) else " ")
            i += n
        }
        val out = sb.toString()
            .replace(spaces, " ")
            .trim()
            .trim('-', '|', ':', '.', ',', '/', ' ')
            .replace(spaces, " ")
        if (out.isEmpty()) return raw.trim()
        return prettyCase(out)
    }
}
