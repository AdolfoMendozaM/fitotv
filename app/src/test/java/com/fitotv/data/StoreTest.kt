package com.fitotv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class StoreTest {
    @Test
    fun guardaYLeeCanalesYGuia() {
        val dir = Files.createTempDirectory("fitotv").toFile()
        val store = Store(dir)
        val chans = (1..5000).map { i ->
            Channel(
                id = "id$i", num = i, name = "Canal $i ñ", url = "http://srv/live/u/p/$i.ts", logo = "http://logo/$i.png",
                group = "Grupo ${i % 7}", tvgId = "t$i", kind = ContentKind.entries[i % 3],
                seriesId = if (i % 3 == 2) "m:Serie ${i % 11}" else "", season = i % 4, episode = i % 30,
            )
        }
        store.saveChannels("L", chans)
        assertEquals(chans, store.loadChannels("L"))
        val epg = mapOf("t1" to listOf(Programme(1, 2, "Noticias", "Desc")), "t2" to emptyList())
        store.saveEpg("L", epg)
        assertEquals(epg, store.loadEpg("L"))
        dir.deleteRecursively()
    }
}
