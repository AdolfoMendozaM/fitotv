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
    fun nuncaMuestraP2AunqueSeRepitaElCanal() {
        val out = clean("P1 - Canal 5", "P1 - Azteca 7", "P1 - Latina", "P2 - Canal 5", "P2 - Azteca 7", "P2 - Latina")
        assertEquals(listOf("Canal 5", "Azteca 7", "Latina", "Canal 5", "Azteca 7", "Latina"), out)
    }

    @Test
    fun distinguePorPais() {
        val out = clean("MX: ESPN", "MX: Fox", "MX: TNT", "US: ESPN", "US: Fox", "US: TNT")
        assertEquals("ESPN · MX", out[0])
        assertEquals("ESPN · US", out[3])
    }

    @Test
    fun quitaP2ConSeparadoresRaros() {
        val out = clean("P2\u00A0- Canal 5", "P2 − Azteca 7", "P2 ➤ Latina", "P2: TNT", "P2 Warner", "P2-Star")
        assertEquals(listOf("Canal 5", "Azteca 7", "Latina", "TNT", "Warner", "Star"), out)
    }

    @Test
    fun quitaP2AlFinal() {
        val out = clean("Canal 5 P2", "Azteca 7 (P2)", "Latina [P2]", "TNT - P2", "Sky Sports F1")
        assertEquals(listOf("Canal 5", "Azteca 7", "Latina", "TNT", "Sky Sports F1"), out)
    }

    @Test
    fun respetaNumerosDeCanal() {
        val out = clean("ESPN 2", "Fox Sports 2", "Canal 5", "A&E", "E! Entertainment", "PPV 1 - UFC", "Telemundo 51")
        assertEquals(listOf("ESPN 2", "Fox Sports 2", "Canal 5", "A&E", "E! Entertainment", "PPV 1 - UFC", "Telemundo 51"), out)
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

    @Test
    fun quitaCategoriaYLetrasDisfrazadas() {
        val names = listOf(
            "CINE | *HBO* F@MILY SD", "CINE | *HBÖ* 2 HD", "CINE | *C1NEMAX* HD", "CINE | A&E FHD",
            "CINE | STAR CHANNEL HD", "CINE | TNT SERIES FHD", "CINE | WARNER FHD",
        )
        val input = names.mapIndexed { i, n -> Channel(id = "c$i", num = i, name = n, url = "u$i", group = "⭐CINE PREMIUM") }
        val out = ChannelNames.clean(input).map { it.name }
        assertEquals(
            listOf("HBO Family SD", "HBO 2 HD", "Cinemax HD", "A&E FHD", "Star Channel HD", "TNT Series FHD", "Warner FHD"),
            out,
        )
    }

    @Test
    fun categoriaSinRelacionConElGrupoSeRespeta() {
        val input = listOf("TNT | Series", "TNT | Novelas", "TNT | Sports").mapIndexed { i, n -> Channel(id = "c$i", num = i, name = n, url = "u$i", group = "Entretenimiento") }
        assertEquals(listOf("TNT | Series", "TNT | Novelas", "TNT | Sports"), ChannelNames.clean(input).map { it.name })
    }

    @Test
    fun nombresEnMayusculas() {
        assertEquals("Canal de las Estrellas", NameText.prettyCase("CANAL DE LAS ESTRELLAS"))
        assertEquals("ESPN 2 HD", NameText.prettyCase("ESPN 2 HD"))
        assertEquals("CNN en Español", NameText.prettyCase("CNN EN ESPAÑOL"))
        assertEquals("Discovery Kids", NameText.prettyCase("Discovery Kids"))
    }

    @Test
    fun gruposSinEmojis() {
        assertEquals("Cine Premium", NameText.cleanGroup("⭐CINE PREMIUM"))
        assertEquals("Copa Libertadores y Sudamericana 2026", NameText.cleanGroup("✨ COPA LIBERTADORES Y SUDAMERICANA 2026 ✨"))
        assertEquals("Eventos UFC", NameText.cleanGroup("⭐Eventos UFC ⭐"))
        assertEquals("Fútbol Chile", NameText.cleanGroup("⚽ FÚTBOL CHILE"))
        assertEquals("Deportes PE", NameText.cleanGroup("⭐DƏPORTES PE"))
        assertEquals("Películas Acción", NameText.cleanGroup("Películas Acción"))
        assertEquals("Terror", NameText.cleanGroup("🎬 TERROR 🔥"))
    }

    @Test
    fun clavesDeGuiaPorNombre() {
        assertEquals("hbo family", EpgNames.key("HBO Family SD"))
        assertEquals(EpgNames.key("Warner Channel"), EpgNames.key("Warner FHD"))
        assertEquals(EpgNames.key("A&E"), EpgNames.key("A&E FHD"))
        assertEquals("canal 5", EpgNames.key("Canal 5 HD"))
        assertEquals("", EpgNames.key("123"))
        assertEquals("cnn espanol", EpgNames.key("CNN en Español"))
    }
}
