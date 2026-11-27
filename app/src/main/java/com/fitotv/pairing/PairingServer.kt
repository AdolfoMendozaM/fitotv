package com.fitotv.pairing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.fitotv.data.ListKind
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/** Lo que llega desde el celular. */
data class PairedList(
    val name: String,
    val kind: ListKind,
    val url: String = "",
    val fileText: String? = null,
    val server: String = "",
    val user: String = "",
    val pass: String = "",
)

/**
 * Pequeño servidor dentro de la tele. El celular (en la misma red Wi-Fi) abre la
 * dirección del QR, pega su lista y la tele la recibe sola. No pasa por internet.
 */
class PairingServer(
    private val context: Context,
    port: Int,
    private val onList: (PairedList) -> Unit,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.method == Method.GET && (session.uri == "/" || session.uri == "/index.html") ->
                    html(page())
                session.method == Method.POST && session.uri == "/lista" -> receive(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No encontrado")
            }
        } catch (e: Exception) {
            html(result(false, "Algo salió mal. Intenta otra vez."))
        }
    }

    private fun receive(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)
        val p = session.parameters.mapValues { it.value.firstOrNull().orEmpty() }
        val name = p["nombre"].orEmpty().trim().ifEmpty { "Mi lista" }.take(40)
        val list = when (p["tipo"]) {
            "enlace" -> {
                val url = p["enlace"].orEmpty().trim()
                if (url.isEmpty()) return html(result(false, "Falta el enlace de la lista."))
                PairedList(name, ListKind.URL, url = url)
            }
            "archivo" -> {
                // El celular lee el archivo y lo manda como texto (así no se pierden los acentos).
                val text = p["contenido"].orEmpty().ifEmpty { files["archivo"]?.let { File(it).readText() }.orEmpty() }
                if (text.isBlank()) return html(result(false, "Falta elegir el archivo."))
                if (!text.contains("#EXT", ignoreCase = true) && !text.contains("http", ignoreCase = true)) {
                    return html(result(false, "Ese archivo no parece una lista M3U."))
                }
                PairedList(name, ListKind.FILE, fileText = text)
            }
            "usuario" -> {
                val server = p["servidor"].orEmpty().trim()
                val user = p["usuario"].orEmpty().trim()
                val pass = p["contrasena"].orEmpty()
                if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                    return html(result(false, "Faltan datos: servidor, usuario y contraseña."))
                }
                PairedList(name, ListKind.XTREAM, server = server, user = user, pass = pass)
            }
            else -> return html(result(false, "Elige cómo quieres agregar tu lista."))
        }
        onList(list)
        return html(result(true, "Listo. Tu lista \"${escape(name)}\" ya va en camino a la tele."))
    }

    private fun html(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", body)

    private fun page(): String = context.assets.open("pair.html").bufferedReader().use { it.readText() }

    private fun result(ok: Boolean, message: String): String = page()
        .replace("<!--RESULT-->", """<div class="result ${if (ok) "ok" else "err"}">$message</div>""")

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        const val PORT = 8090

        /** La dirección de la tele en la red de la casa. */
        fun localAddress(): String? = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }

        fun qr(text: String, size: Int = 360): Bitmap {
            val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
            val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(m.width * m.height)
            for (y in 0 until m.height) for (x in 0 until m.width) {
                pixels[y * m.width + x] = if (m.get(x, y)) Color.rgb(10, 23, 71) else Color.WHITE
            }
            return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
        }
    }
}
