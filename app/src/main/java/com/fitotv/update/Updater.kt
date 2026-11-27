package com.fitotv.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.fitotv.BuildConfig
import com.fitotv.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

/** Lo que publica GitHub junto a cada versión (version.json). */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String = "",
    val apk: String = Updater.APK_URL,
)

/**
 * Actualizaciones desde la app: pregunta a GitHub si hay una versión más nueva,
 * baja el instalable y abre el instalador del sistema. Se instala encima, sin borrar perfiles ni listas.
 */
class Updater(private val context: Context) {

    companion object {
        private const val BASE = "https://github.com/AdolfoMendozaM/fitotv/releases/latest/download"
        const val VERSION_URL = "$BASE/version.json"
        const val APK_URL = "$BASE/FitoTV.apk"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** La versión nueva si existe; null si ya tienes la más reciente o no se pudo revisar. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            Http.client.newCall(Request.Builder().url(VERSION_URL).header("Cache-Control", "no-cache").build()).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val info = json.decodeFromString(UpdateInfo.serializer(), res.body!!.string())
                info.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
            }
        }.getOrNull()
    }

    /** Baja el instalable a la memoria temporal de la app, avisando el avance (0 a 1). */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "FitoTV-${info.versionName}.apk")
        Http.client.newCall(Request.Builder().url(info.apk).build()).execute().use { res ->
            if (!res.isSuccessful) error("GitHub respondió ${res.code}")
            val body = res.body ?: error("Descarga vacía")
            val total = body.contentLength()
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var last = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0 && done - last > 256 * 1024) {
                            last = done
                            onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        onProgress(1f)
        out
    }

    /** En Android 8+ cada app necesita permiso para instalar otras; en Fire TV se da una vez. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla del permiso si el sistema la tiene; false si hay que ir a mano. */
    fun openInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /** Abre el instalador del sistema con el archivo bajado. */
    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
