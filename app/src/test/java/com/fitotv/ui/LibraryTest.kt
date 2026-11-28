package com.fitotv.ui

import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private val m3u = """
        #EXTM3U
        #EXTINF:-1 group-title="Noticias",Canal 24
        http://s/live/u/p/1.ts
        #EXTINF:-1 group-title="Noticias",Canal 24
        http://s/live/u/p/1.ts
        #EXTINF:-1 group-title="Series Paramount",El Chema S01 E02
        http://s/series/u/p/12.mp4
        #EXTINF:-1 group-title="Series Paramount",El Chema S01 E01
        http://s/series/u/p/11.mp4
        #EXTINF:-1 group-title="Series Paramount",El Chema S02 E01
        http://s/series/u/p/21.mp4
        #EXTINF:-1 group-title="Series Acción",Lost S1 E1
        http://s/series/u/p/31.mp4
        #EXTINF:-1 group-title="Películas",Duro de matar (1988)
        http://s/movie/u/p/41.mkv
    """.trimIndent()

    @Test
    fun episodiosAgrupadosPorSerie() {
        val raw = M3uParser.parse(m3u.reader().buffered(), "L").channels
        val lib = Library.build(raw, kids = false)
        assertEquals(listOf("El Chema", "Lost"), lib.shows.items.map { it.name })
        val chema = lib.shows.items[0]
        assertTrue(chema.isShow)
        val eps = lib.episodesByShow[chema.seriesId]!!
        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1), eps.map { it.season to it.episode })
        assertEquals(1, lib.movies.items.size)
        assertEquals(listOf("Noticias"), lib.groups.map { it.name })
        assertEquals(listOf("El Chema"), lib.search("chema").map { it.name })
        assertEquals("Duro de matar (1988)", lib.search("DURO")[0].name)
    }

    @Test
    fun idRepetidoUnaSolaVez() {
        val raw = listOf(
            Channel("a", 1, "Uno", "http://x/1", group = "G"),
            Channel("a", 1, "Uno", "http://x/1", group = "G"),
        )
        assertEquals(1, Library.build(raw, kids = false).live.size)
    }

    @Test
    fun perfilInfantil() {
        val raw = listOf(
            Channel("a", 1, "Caricaturas 1", "http://x/1", group = "Infantil"),
            Channel("b", 2, "Noticias", "http://x/2", group = "Noticias"),
        )
        val lib = Library.build(raw, kids = true)
        assertEquals(listOf("a"), lib.live.map { it.id })
        assertEquals(ContentKind.LIVE, lib.live[0].kind)
    }
}
