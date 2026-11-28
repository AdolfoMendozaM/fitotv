package com.fitotv.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeToSequence
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Listas de usuario y contraseña (Xtream) leídas con su API en lugar del M3U gigante.
 *
 * - Llega una entrada por serie (no una por episodio): las temporadas se piden al abrirla.
 * - Los JSON se leen elemento por elemento, sin cargar el archivo completo en memoria.
 * - Si el servidor no tiene la API, quien llama usa el M3U de siempre.
 */
@OptIn(ExperimentalSerializationApi::class)
object XtreamApi {

    /** El servidor no contestó como Xtream: hay que usar el M3U. */
    class NotXtream : IOException("El servidor no tiene la API de Xtream")

    /** La cuenta tiene un problema (contraseña, vencida): no tiene caso intentar el M3U. */
    class AccountProblem(message: String) : IOException(message)

    data class Creds(val server: String, val user: String, val pass: String) {
        val base get() = Xtream.normalizeServer(server)
        fun api(action: String? = null, extra: String = ""): String =
            "$base/player_api.php?username=${enc(user)}&password=${enc(pass)}" +
                (if (action != null) "&action=$action" else "") + extra
    }

    data class Season(val number: Int, val name: String, val episodes: List<Channel>)
    data class SeriesInfo(val plot: String, val cover: String, val year: String, val seasons: List<Season>)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")

    /** "http://srv:8080/get.php?username=a&password=b&type=m3u_plus" → datos de Xtream. */
    fun credsFromUrl(url: String): Creds? {
        return try {
            val u = URI(url.trim())
            val path = u.path?.lowercase() ?: return null
            if (!path.endsWith("/get.php") && !path.endsWith("/player_api.php")) return null
            val q = (u.rawQuery ?: return null).split('&').mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i).lowercase() to URLDecoder.decode(it.substring(i + 1), "UTF-8")
            }.toMap()
            val user = q["username"] ?: return null
            val pass = q["password"] ?: return null
            val port = if (u.port > 0) ":${u.port}" else ""
            val prefix = path.substringBeforeLast('/')
            Creds("${u.scheme}://${u.host}$port$prefix", user, pass)
        } catch (e: Exception) {
            null
        }
    }

    /** Vencimiento, pantallas y estado de la cuenta (una sola petición, ligera). */
    fun account(c: Creds): AccountInfo {
        val account = Http.open(c.api()) { readObject(it) }
        val user = account["user_info"] as? JsonObject ?: throw NotXtream()
        return accountOf(user)
    }

    private fun accountOf(user: JsonObject): AccountInfo {
        val exp = user.str("exp_date").toLongOrNull()
        return AccountInfo(
            // Sin fecha (null o 0) es una cuenta sin vencimiento.
            expiresAt = if (exp == null || exp <= 0) -1 else exp * 1000,
            maxScreens = user.str("max_connections").toIntOrNull() ?: 0,
            activeScreens = user.str("active_cons").toIntOrNull() ?: 0,
            status = user.str("status"),
            trial = user.str("is_trial") == "1",
        )
    }

    /** Revisa la cuenta y baja canales, películas y series. */
    fun load(c: Creds): M3uParser.Result {
        val account = Http.open(c.api()) { readObject(it) }
        val user = account["user_info"] as? JsonObject ?: throw NotXtream()
        if (user.str("auth") == "0") {
            throw AccountProblem("El proveedor no dejó entrar. Revisa el usuario y la contraseña.")
        }
        when (user.str("status").lowercase()) {
            "", "active" -> Unit
            "expired" -> throw AccountProblem("La cuenta de tu proveedor venció. Renuévala con él para volver a ver.")
            "banned", "disabled" -> throw AccountProblem("Tu proveedor desactivó la cuenta.")
        }
        val formats = (user["allowed_output_formats"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
        val liveExt = if (formats.isEmpty() || "ts" in formats) "ts" else "m3u8"

        val out = ArrayList<Channel>(32_768)
        out += section(c, "get_live_categories", "get_live_streams") { o, group ->
            val id = o.str("stream_id").ifEmpty { return@section null }
            Channel(
                id = "xl$id",
                num = o.str("num").toIntOrNull() ?: 0,
                name = o.str("name").trim().ifEmpty { "Canal $id" },
                url = "${c.base}/live/${c.user}/${c.pass}/$id.$liveExt",
                logo = o.str("stream_icon").trim(),
                group = group,
                tvgId = o.str("epg_channel_id").trim(),
                kind = ContentKind.LIVE,
            )
        }
        out += section(c, "get_vod_categories", "get_vod_streams") { o, group ->
            val id = o.str("stream_id").ifEmpty { return@section null }
            val ext = o.str("container_extension").ifEmpty { "mp4" }
            Channel(
                id = "xm$id",
                num = 0,
                name = o.str("name").trim().ifEmpty { "Película $id" },
                url = "${c.base}/movie/${c.user}/${c.pass}/$id.$ext",
                logo = o.str("stream_icon").trim(),
                group = group,
                kind = ContentKind.MOVIE,
                added = o.str("added").toLongOrNull() ?: 0,
            )
        }
        out += section(c, "get_series_categories", "get_series") { o, group ->
            val id = o.str("series_id").ifEmpty { return@section null }
            Channel(
                id = "xs$id",
                num = 0,
                name = o.str("name").trim().ifEmpty { "Serie $id" },
                url = "",
                logo = o.str("cover").trim(),
                group = group,
                kind = ContentKind.SERIES,
                seriesId = "x:$id",
                // La fecha del último episodio subido: así las series con capítulos nuevos salen primero.
                added = o.str("last_modified").toLongOrNull() ?: o.str("added").toLongOrNull() ?: 0,
            )
        }
        // Los canales en vivo llevan número consecutivo si el proveedor no lo manda.
        var n = 0
        val numbered = out.map { ch ->
            if (ch.kind != ContentKind.LIVE) ch else {
                n += 1
                if (ch.num > 0) ch else ch.copy(num = n)
            }
        }
        return M3uParser.Result(numbered, Xtream.epgUrl(c.server, c.user, c.pass), accountOf(user))
    }

    /**
     * Una sección (canales, películas o series) ordenada como las categorías del proveedor.
     * Si una sección falla (algunos proveedores no tienen series), se sigue con las demás.
     */
    private fun section(
        c: Creds,
        catAction: String,
        itemsAction: String,
        build: (JsonObject, String) -> Channel?,
    ): List<Channel> {
        val cats = LinkedHashMap<String, String>()
        try {
            Http.open(c.api(catAction)) { input ->
                readArray(input) { o -> cats[o.str("category_id")] = o.str("category_name").trim().ifEmpty { "Sin grupo" } }
            }
        } catch (e: Exception) {
            // sin categorías: todo cae en "Sin grupo"
        }
        val buckets = LinkedHashMap<String, MutableList<Channel>>()
        cats.values.forEach { buckets.getOrPut(it) { ArrayList() } }
        try {
            Http.open(c.api(itemsAction)) { input ->
                readArray(input) { o ->
                    val group = cats[o.str("category_id")] ?: "Sin grupo"
                    build(o, group)?.let { buckets.getOrPut(group) { ArrayList() }.add(it) }
                }
            }
        } catch (e: Exception) {
            if (itemsAction == "get_live_streams") throw e
        }
        return buckets.values.flatten()
    }

    /** Temporadas y episodios de una serie (se pide al abrirla). */
    fun seriesInfo(c: Creds, show: Channel): SeriesInfo {
        val sid = show.seriesId.removePrefix("x:")
        val root = Http.open(c.api("get_series_info", "&series_id=$sid")) { readObject(it) }
        val info = root["info"] as? JsonObject
        val cover = info?.str("cover").orEmpty().ifEmpty { show.logo }
        val names = HashMap<Int, String>()
        (root["seasons"] as? JsonArray)?.forEach { s ->
            val o = s as? JsonObject ?: return@forEach
            val num = o.str("season_number").toIntOrNull() ?: return@forEach
            names[num] = o.str("name")
        }
        val bySeason = sortedMapOf<Int, MutableList<Channel>>()
        fun addEpisode(seasonKey: Int?, e: JsonObject) {
            val id = e.str("id").ifEmpty { return }
            val season = e.str("season").toIntOrNull() ?: seasonKey ?: 1
            val num = e.str("episode_num").toIntOrNull() ?: 0
            val ext = e.str("container_extension").ifEmpty { "mp4" }
            val ei = e["info"] as? JsonObject
            bySeason.getOrPut(season) { ArrayList() }.add(
                Channel(
                    id = "xe$id",
                    num = 0,
                    name = e.str("title").trim().ifEmpty { "${show.name} T$season E$num" },
                    url = "${c.base}/series/${c.user}/${c.pass}/$id.$ext",
                    logo = ei?.str("movie_image").orEmpty().ifEmpty { cover },
                    group = show.name,
                    kind = ContentKind.SERIES,
                    seriesId = show.seriesId,
                    season = season,
                    episode = num,
                )
            )
        }
        when (val eps = root["episodes"]) {
            is JsonObject -> eps.forEach { (k, v) -> (v as? JsonArray)?.forEach { e -> (e as? JsonObject)?.let { addEpisode(k.toIntOrNull(), it) } } }
            is JsonArray -> eps.forEach { group ->
                when (group) {
                    is JsonArray -> group.forEach { e -> (e as? JsonObject)?.let { addEpisode(null, it) } }
                    is JsonObject -> addEpisode(null, group)
                    else -> Unit
                }
            }
            else -> Unit
        }
        val seasons = bySeason.map { (num, list) ->
            val name = names[num]?.takeIf { it.isNotBlank() && !it.equals("Season $num", true) } ?: "Temporada $num"
            Season(num, name, list.sortedBy { it.episode })
        }
        return SeriesInfo(
            plot = info?.str("plot").orEmpty(),
            cover = cover,
            year = info?.str("releaseDate").orEmpty().ifEmpty { info?.str("release_date").orEmpty() }.take(4),
            seasons = seasons,
        )
    }

    /** Lo que pasa ahora y después en un canal (guía corta del proveedor, títulos en base64). */
    fun shortEpg(c: Creds, streamId: String): List<Programme> {
        val root = Http.open(c.api("get_short_epg", "&stream_id=$streamId&limit=12")) { readObject(it) }
        val list = root["epg_listings"] as? JsonArray ?: return emptyList()
        fun b64(s: String): String = try {
            String(android.util.Base64.decode(s, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            s
        }
        return list.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val start = o.str("start_timestamp").toLongOrNull() ?: return@mapNotNull null
            val stop = o.str("stop_timestamp").toLongOrNull() ?: return@mapNotNull null
            Programme(start * 1000, stop * 1000, b64(o.str("title")).ifEmpty { "Sin título" }, b64(o.str("description")).take(300))
        }.sortedBy { it.start }
    }

    // ----- Lectura de JSON -----

    private fun readObject(input: InputStream): JsonObject {
        val el = try {
            json.decodeFromString(JsonElement.serializer(), input.bufferedReader().readText())
        } catch (e: Exception) {
            throw NotXtream()
        }
        return el as? JsonObject ?: throw NotXtream()
    }

    /** Recorre un arreglo JSON grande sin cargarlo completo. */
    private fun readArray(input: InputStream, each: (JsonObject) -> Unit) {
        val buffered = input.buffered()
        // Algunos proveedores contestan {} o null cuando no hay nada.
        buffered.mark(64)
        var first = buffered.read()
        while (first == ' '.code || first == '\n'.code || first == '\r'.code || first == '\t'.code || first == 0xEF || first == 0xBB || first == 0xBF) {
            first = buffered.read()
        }
        buffered.reset()
        if (first != '['.code) return
        json.decodeToSequence(buffered, JsonElement.serializer(), DecodeSequenceMode.ARRAY_WRAPPED).forEach { el ->
            if (el is JsonObject) each(el)
        }
    }

    private fun JsonObject.str(key: String): String = when (val v = this[key]) {
        null, is JsonNull -> ""
        is JsonPrimitive -> v.content
        else -> ""
    }
}
