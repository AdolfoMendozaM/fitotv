package com.fitotv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelNamesTest {

    private fun ch(i: Int, name: String) = Channel(id = "c$i", num = i, name = name, url = "http://x/$i")

    private fun clean(vararg names: String): List<String> =
        ChannelNames.clean(names.mapIndexed { i, n -> ch(i, n) }).map { it.name }

    @Test
    fun quitaEtiquetaRepetidaDelProveedor() {
        val out = clean("≡P2 - Canal 5", "≡P2 - Azteca 7", "≡P2 - Latina", "≡P2 - Warner Channel")
        assertEquals(listOf("Canal 5", "Azteca 7", "Latina", "Warner Channel"), out)
    }

    @Test
    fun quitaPaisYCorchetes() {
        val out = clean("MX: Las Estrellas", "MX: Canal Once", "MX: Imagen TV", "[LAT] Discovery", "[LAT] History", "|MX| Milenio", "|MX| Foro TV")
        assertEquals(listOf("Las Estrellas", "Canal Once", "Imagen TV", "Discovery", "History", "Milenio", "Foro TV"), out)
    }

    @Test
    fun respetaNombresPropios() {
        val out = clean("TNT - Series", "TNT - Novelas", "TNT - Sports", "PPV 1 - UFC", "PPV 2 - Box", "Canal 22")
        assertEquals(listOf("TNT - Series", "TNT - Novelas", "TNT - Sports", "PPV 1 - UFC", "PPV 2 - Box", "Canal 22"), out)
    }

    @Test
    fun quitaEmojisDelProveedor() {
        val out = clean("🟢 P2 - Warner Bros TV", "🟢 P2 - TNT", "🟢 P2 - Star Channel", "⭐ Disney+ ⭐")
        assertEquals(listOf("Warner Bros TV", "TNT", "Star Channel", "Disney+"), out)
    }

    @Test
    fun quitaAdornos() {
        assertEquals(listOf("ESPN 2 HD"), clean("★ ESPN 2 HD ★"))
    }

    @Test
    fun distingueCanalesQueQuedanIguales() {
        val out = clean("P1 - Canal 5", "P1 - Azteca 7", "P1 - Latina", "P2 - Canal 5", "P2 - Azteca 7", "P2 - Latina")
        assertEquals("Canal 5 · P1", out[0])
        assertEquals("Canal 5 · P2", out[3])
    }

    @Test
    fun quitaResolucionYNotasRepetidas() {
        val out = clean(
            "Canal 13 Tabasco (720p) [Not 24/7]", "Canal 28 (720p) [Not 24/7]", "Despierta TV (1080p) [Not 24/7]",
            "Imagen TV (1080p)", "Canal 22 [Señal MX]",
        )
        assertEquals(listOf("Canal 13 Tabasco", "Canal 28", "Despierta TV", "Imagen TV", "Canal 22 [Señal MX]"), out)
    }

    @Test
    fun distingueResoluciones() {
        val out = clean("Canal 5 (720p)", "Canal 5 (1080p)", "Azteca 7 (1080p)")
        assertEquals(listOf("Canal 5 · 720p", "Canal 5 · 1080p", "Azteca 7"), out)
    }

    @Test
    fun noCambiaLosId() {
        val input = listOf(ch(1, "≡P2 - Canal 5"), ch(2, "≡P2 - Latina"), ch(3, "≡P2 - TNT"))
        assertEquals(input.map { it.id }, ChannelNames.clean(input).map { it.id })
    }
}
