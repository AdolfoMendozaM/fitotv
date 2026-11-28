package com.fitotv.data

import java.text.Normalizer
import java.util.Locale

/** Comparar textos sin importar mayúsculas ni acentos. */
object Fold {
    private val marks = Regex("\\p{Mn}+")

    private val kidsWords = listOf("infantil", "kids", "nino", "ninos", "children", "cartoon", "caricatura", "animad", "familia")

    /** Minúsculas y sin acentos. La mayoría de los nombres son ASCII: esos no pasan por la normalización. */
    fun fold(s: String): String {
        val low = s.lowercase(Locale.ROOT)
        for (c in low) if (c.code > 127) return Normalizer.normalize(low, Normalizer.Form.NFD).replace(marks, "")
        return low
    }

    fun isKidsGroup(group: String): Boolean {
        val g = fold(group)
        return kidsWords.any { g.contains(it) }
    }
}
