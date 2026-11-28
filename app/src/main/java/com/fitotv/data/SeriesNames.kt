package com.fitotv.data

/**
 * Separa el nombre de un episodio en serie, temporada, episodio y título:
 * "El Chema S01 E05 - El regreso" → ("El Chema", 1, 5, "El regreso").
 *
 * Reconoce S01E05, S1 E5, T1 E5, 1x05, "Temporada 1 Capítulo 5" y "Season 1 Episode 5".
 * Las listas M3U traen cada episodio suelto; con esto se agrupan por serie y temporada.
 */
object SeriesNames {

    data class Ep(val show: String, val season: Int, val episode: Int, val title: String)

    private const val SEP = """[\s._\-–—|:,(\[]*"""

    private val patterns = listOf(
        Regex("""^(.*?)$SEP[STst](\d{1,2})[\s._\-]*[Ee][Pp]?(\d{1,4})(?!\d)(.*)$"""),
        Regex("""^(.*?)$SEP(?<!\d)(\d{1,2})[xX](\d{2,3})(?!\d)(.*)$"""),
        Regex("""^(.*?)${SEP}[Tt]emporada\s*(\d{1,2})$SEP(?:[Cc]ap[ií]tulo|[Ee]pisodio|[Ee]p\.?|[Ee])\s*(\d{1,4})(.*)$"""),
        Regex("""^(.*?)${SEP}[Ss]eason\s*(\d{1,2})$SEP(?:[Ee]pisode|[Ee]p\.?|[Ee])\s*(\d{1,4})(.*)$"""),
    )

    private const val TRIM = " -–—|:._([,"

    fun parse(name: String): Ep? {
        for (re in patterns) {
            val m = re.find(name) ?: continue
            val show = m.groupValues[1].trim().trimEnd(*TRIM.toCharArray()).trim()
            if (show.isEmpty()) continue
            val season = m.groupValues[2].toIntOrNull() ?: continue
            val episode = m.groupValues[3].toIntOrNull() ?: continue
            val title = m.groupValues[4].trim().trimStart(*TRIM.toCharArray()).trimEnd(')', ']').trim()
            return Ep(show, season, episode, title)
        }
        return null
    }
}
