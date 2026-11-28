package com.fitotv.data

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress

/** Un servidor Xtream de mentira en la compu de pruebas. */
class XtreamApiTest {
    private lateinit var server: HttpServer
    private var base = ""
    private var status = "Active"
    private var vodCount = 3

    @Before
    fun up() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/player_api.php") { ex ->
            val q = ex.requestURI.rawQuery.orEmpty()
            val action = Regex("action=([a-z_]+)").find(q)?.groupValues?.get(1)
            val body = when (action) {
                null -> """{"user_info":{"auth":1,"status":"$status","allowed_output_formats":["m3u8","ts"]},"server_info":{}}"""
                "get_live_categories" -> """[{"category_id":"1","category_name":"Noticias"},{"category_id":"2","category_name":"Deportes"}]"""
                "get_live_streams" -> """[{"num":1,"name":"Deportes 1","stream_id":20,"stream_icon":"","epg_channel_id":"dep1","category_id":"2"},
                    {"num":2,"name":"Noticias 24","stream_id":"10","stream_icon":"http://l/n.png","epg_channel_id":null,"category_id":1}]"""
                "get_vod_categories" -> """[{"category_id":"5","category_name":"Acción"}]"""
                "get_vod_streams" -> buildString {
                    append('[')
                    for (i in 1..vodCount) {
                        if (i > 1) append(',')
                        append("""{"name":"Peli $i (2020)","stream_id":${1000 + i},"stream_icon":"http://p/$i.jpg","category_id":"5","container_extension":"mkv"}""")
                    }
                    append(']')
                }
                "get_series_categories" -> """[{"category_id":"9","category_name":"Series Paramount"}]"""
                "get_series" -> """[{"name":"El Chema","series_id":77,"cover":"http://c/77.jpg","category_id":"9"}]"""
                "get_series_info" -> """{"info":{"plot":"Narco","cover":"http://c/77big.jpg","releaseDate":"2016-05-01"},
                    "seasons":[{"season_number":1,"name":"Season 1"}],
                    "episodes":{"1":[{"id":"501","episode_num":2,"title":"El Chema S01 E02","container_extension":"mp4","info":{"movie_image":"http://i/2.jpg"}},
                                     {"id":"500","episode_num":1,"title":"El Chema S01 E01","container_extension":"mp4","info":{}}],
                                "2":[{"id":"600","episode_num":1,"title":"El Chema S02 E01","container_extension":"mkv"}]}}"""
                else -> "[]"
            }
            val bytes = body.toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        base = "http://127.0.0.1:${server.address.port}"
    }

    @After
    fun down() = server.stop(0)

    @Test
    fun cargaCanalesPeliculasYSeries() {
        val r = XtreamApi.load(XtreamApi.Creds(base, "u", "p"))
        val live = r.channels.filter { it.kind == ContentKind.LIVE }
        // En el orden de las categorías del proveedor: Noticias primero.
        assertEquals(listOf("Noticias 24", "Deportes 1"), live.map { it.name })
        assertEquals("$base/live/u/p/10.ts", live[0].url)
        assertEquals("dep1", live[1].tvgId)
        val movies = r.channels.filter { it.kind == ContentKind.MOVIE }
        assertEquals(3, movies.size)
        assertEquals("$base/movie/u/p/1001.mkv", movies[0].url)
        assertEquals("Acción", movies[0].group)
        val shows = r.channels.filter { it.kind == ContentKind.SERIES }
        assertEquals(1, shows.size)
        assertTrue(shows[0].isShow)
        assertEquals("x:77", shows[0].seriesId)

        val info = XtreamApi.seriesInfo(XtreamApi.Creds(base, "u", "p"), shows[0])
        assertEquals(listOf(1, 2), info.seasons.map { it.number })
        assertEquals("Temporada 1", info.seasons[0].name)
        assertEquals(listOf(1, 2), info.seasons[0].episodes.map { it.episode })
        assertEquals("$base/series/u/p/500.mp4", info.seasons[0].episodes[0].url)
        assertEquals("http://c/77big.jpg", info.seasons[0].episodes[0].logo)
        assertEquals("2016", info.year)
    }

    @Test
    fun cuentaVencida() {
        status = "Expired"
        try {
            XtreamApi.load(XtreamApi.Creds(base, "u", "p"))
            fail()
        } catch (e: XtreamApi.AccountProblem) {
            assertTrue(e.message!!.contains("venció"))
        }
    }

    @Test
    fun listaEnormeSinLlenarLaMemoria() {
        vodCount = 200_000
        val rt = Runtime.getRuntime()
        rt.gc()
        val t0 = System.nanoTime()
        val r = XtreamApi.load(XtreamApi.Creds(base, "u", "p"))
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertEquals(200_000, r.channels.count { it.kind == ContentKind.MOVIE })
        println("200 mil películas en $ms ms")
    }

    @Test
    fun enlaceGetPhp() {
        val c = XtreamApi.credsFromUrl("http://starlatino.example:8880/get.php?username=ana&password=x%2By&type=m3u_plus&output=ts")!!
        assertEquals("http://starlatino.example:8880", c.server)
        assertEquals("ana", c.user)
        assertEquals("x+y", c.pass)
        assertEquals(null, XtreamApi.credsFromUrl("http://srv/lista.m3u"))
    }

    @Test(expected = IOException::class)
    fun noEsXtream() {
        XtreamApi.load(XtreamApi.Creds("$base/nada", "u", "p"))
    }
}
