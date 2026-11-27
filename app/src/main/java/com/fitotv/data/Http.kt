package com.fitotv.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object Http {
    const val USER_AGENT = "FitoTV/1.0 (Linux; Android TV)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val req = chain.request()
            val withUa = if (req.header("User-Agent") == null) {
                req.newBuilder().header("User-Agent", USER_AGENT).build()
            } else req
            chain.proceed(withUa)
        }
        .build()

    /** Abre una dirección y entrega el contenido, descomprimiendo si viene en .gz. */
    fun <T> open(url: String, block: (InputStream) -> T): T {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException(httpMessage(response.code))
            val body = response.body ?: throw IOException("La lista llegó vacía")
            val raw = BufferedInputStream(body.byteStream())
            raw.mark(4)
            val b1 = raw.read()
            val b2 = raw.read()
            raw.reset()
            val stream: InputStream = if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(raw) else raw
            return block(stream)
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401, 403 -> "El servidor no dio permiso (usuario o contraseña incorrectos, o la cuenta venció)"
        404 -> "No encontramos la lista en esa dirección"
        in 500..599 -> "El servidor de la lista no está respondiendo"
        else -> "El servidor respondió con un error ($code)"
    }
}

object Xtream {
    fun normalizeServer(server: String): String {
        var s = server.trim().trimEnd('/')
        if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
        return s
    }

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")

    fun m3uUrl(server: String, user: String, pass: String): String =
        "${normalizeServer(server)}/get.php?username=${enc(user)}&password=${enc(pass)}&type=m3u_plus&output=ts"

    fun epgUrl(server: String, user: String, pass: String): String =
        "${normalizeServer(server)}/xmltv.php?username=${enc(user)}&password=${enc(pass)}"
}
