package com.fitotv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesNamesTest {
    private fun p(name: String) = SeriesNames.parse(name)

    @Test
    fun formasComunes() {
        assertEquals(SeriesNames.Ep("El Chema", 1, 5, ""), p("El Chema S01 E05"))
        assertEquals(SeriesNames.Ep("El Chema", 1, 5, "El regreso"), p("El Chema S01E05 - El regreso"))
        assertEquals(SeriesNames.Ep("Lost", 1, 4, ""), p("Lost S1 E4"))
        assertEquals(SeriesNames.Ep("Rebelde", 2, 10, ""), p("Rebelde T2 E10"))
        assertEquals(SeriesNames.Ep("La casa de papel", 1, 5, ""), p("La casa de papel 1x05"))
        assertEquals(SeriesNames.Ep("La Rosa de Guadalupe", 3, 12, ""), p("La Rosa de Guadalupe - Temporada 3 Capítulo 12"))
        assertEquals(SeriesNames.Ep("Friends", 2, 7, "The One"), p("Friends Season 2 Episode 7 - The One"))
        assertEquals(SeriesNames.Ep("The 100", 2, 3, ""), p("The 100 S02E03"))
        assertEquals(SeriesNames.Ep("Stranger Things", 4, 1, ""), p("Stranger Things (S04E01)"))
    }

    @Test
    fun sinEpisodio() {
        assertNull(p("Película cualquiera (2019)"))
        assertNull(p("S01E01"))
        assertNull(p("Canal 4x4"))
    }
}
