package com.fitotv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val sample = """
        #EXTM3U x-tvg-url="http://epg.example.com/guide.xml.gz,http://otro"
        #EXTINF:-1 tvg-id="noticias24.mx" tvg-name="Noticias 24" tvg-logo="http://logo/n24.png" group-title="Noticias",Noticias 24 HD
        #EXTVLCOPT:http-user-agent=VLC/3.0
        http://srv:8080/live/u/p/123.ts
        #EXTINF:-1 tvg-name="A, B" group-title="Cine, clásico",Cine Uno, con coma
        http://srv:8080/live/u/p/124.m3u8
        #EXTINF:-1 group-title="Películas Acción",Rápido y veloz (2020)
        http://srv:8080/movie/u/p/999.mkv
        #EXTINF:-1 group-title="Series Drama",Barrio Alto S02 E05
        http://srv:8080/series/u/p/555.mp4
        #EXTINF:-1,Sin grupo
        #EXTGRP:Deportes
        https://cdn.example.com/hls/dep/index.m3u8?token=abc
        http://bare.example.com/stream
    """.trimIndent()

    private fun parse() = M3uParser.parse(sample.reader().buffered(), "L1")

    @Test
    fun leeGuiaYCanales() {
        val r = parse()
        assertEquals("http://epg.example.com/guide.xml.gz", r.epgUrl)
        assertEquals(6, r.channels.size)
        val n24 = r.channels[0]
        assertEquals("Noticias 24 HD", n24.name)
        assertEquals("Noticias", n24.group)
        assertEquals("noticias24.mx", n24.tvgId)
        assertEquals("VLC/3.0", n24.userAgent)
        assertEquals(1, n24.num)
    }

    @Test
    fun respetaComasEntreComillas() {
        val c = parse().channels[1]
        assertEquals("Cine Uno, con coma", c.name)
        assertEquals("Cine, clásico", c.group)
    }

    @Test
    fun separaPeliculasSeriesYCanales() {
        val kinds = parse().channels.map { it.kind }
        assertEquals(
            listOf(ContentKind.LIVE, ContentKind.LIVE, ContentKind.MOVIE, ContentKind.SERIES, ContentKind.LIVE, ContentKind.LIVE),
            kinds,
        )
    }

    @Test
    fun usaExtGrpCuandoNoHayGroupTitle() {
        assertEquals("Deportes", parse().channels[4].group)
    }

    @Test
    fun losIdsNoCambianEntreActualizaciones() {
        assertEquals(parse().channels.map { it.id }, parse().channels.map { it.id })
        assertTrue(parse().channels.map { it.id }.toSet().size == 6)
    }
}
