package com.fitotv.data

import kotlinx.serialization.Serializable

/** Cómo se agregó la lista. */
@Serializable
enum class ListKind { URL, FILE, XTREAM }

/** Qué es cada elemento de la lista. */
@Serializable
enum class ContentKind { LIVE, MOVIE, SERIES }

/** Los avatares que vienen con la app (personajes originales). En pantalla no llevan nombre. */
enum class AvatarId(val key: String, val label: String) {
    NINA("nina", "Niña con trenzas y moños"),
    NINO("nino", "Niño con sudadera roja"),
    CHAVA("chava", "Chava con audífonos"),
    CHAVO("chavo", "Chavo con gorra"),
    MAMA("mama", "Mamá con trenzas y aretes"),
    PAPA("papa", "Papá con barba y guayabera"),
    ABUELA("abuela", "Abuelita con lentes y rebozo"),
    ABUELO("abuelo", "Abuelito con sombrero"),
    PERRITO("perrito", "Perrito con paliacate"),
    GATITO("gatito", "Gatito con cascabel"),
    AJOLOTE("ajolote", "Ajolote"),
    XOLO("xolo", "Xoloitzcuintle"),
    JAGUAR("jaguar", "Jaguarcito");

    companion object {
        /** Los perfiles hechos con los avatares anteriores pasan al más parecido. */
        private val legacy = mapOf(
            "audifonos" to CHAVA, "pelo_largo" to MAMA, "gorra" to CHAVO, "chongo" to ABUELA, "coletas" to NINA,
        )

        fun of(key: String?): AvatarId = entries.firstOrNull { it.key == key } ?: legacy[key] ?: NINA
    }
}

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val avatar: String,
    val kids: Boolean = false,
    val activeListId: String? = null,
    val createdAt: Long = 0,
)

@Serializable
data class Playlist(
    val id: String,
    val profileId: String,
    val name: String,
    val kind: ListKind,
    val url: String = "",
    val server: String = "",
    val user: String = "",
    val pass: String = "",
    val epgUrl: String = "",
    val addedAt: Long = 0,
    val updatedAt: Long = 0,
    val channelCount: Int = 0,
    val lastError: String? = null,
    /** Con qué versión de la limpieza se guardaron los nombres (para limpiar otra vez si mejora). */
    val namesVersion: Int = 0,
    /** Cuenta del proveedor (solo usuario y contraseña): vencimiento en ms, 0 si no se sabe, -1 sin vencimiento. */
    val expiresAt: Long = 0,
    /** Cuántas pantallas a la vez permite la cuenta (0 si no se sabe). */
    val maxScreens: Int = 0,
    /** "Active", "Expired", "Banned"… tal como lo dice el proveedor. */
    val accountStatus: String = "",
    val trial: Boolean = false,
    val accountCheckedAt: Long = 0,
    /** Versión de la guía guardada (2: incluye la guía pública por nombre). */
    val epgVersion: Int = 0,
)

/** Lo que dice el proveedor de la cuenta (Xtream). */
data class AccountInfo(
    val expiresAt: Long,
    val maxScreens: Int,
    val activeScreens: Int,
    val status: String,
    val trial: Boolean,
)

@Serializable
data class Channel(
    val id: String,
    val num: Int,
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = "",
    val tvgId: String = "",
    val kind: ContentKind = ContentKind.LIVE,
    val userAgent: String = "",
    val referrer: String = "",
    /**
     * Series: en una serie completa (Xtream) o en un episodio, a qué serie pertenece.
     * "x:123" viene del proveedor (series_id); "m:Nombre" se armó con los nombres de la lista M3U.
     */
    val seriesId: String = "",
    val season: Int = 0,
    val episode: Int = 0,
    /** Cuándo lo subió el proveedor (segundos), si lo dice. Sirve para ordenar de lo más nuevo a lo más viejo. */
    val added: Long = 0,
) {
    /** Una serie completa: se abre para ver sus temporadas en lugar de reproducirse. */
    val isShow: Boolean get() = kind == ContentKind.SERIES && url.isEmpty() && seriesId.isNotEmpty()
}

@Serializable
data class RecentItem(
    val channelId: String,
    val listId: String,
    val kind: ContentKind,
    val at: Long,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** Copia del episodio cuando no forma parte de la lista guardada (episodios de Xtream). */
    val snapshot: Channel? = null,
)

@Serializable
data class ProfileData(
    val recents: List<RecentItem> = emptyList(),
    val favorites: List<String> = emptyList(),
    /** Último grupo elegido en cada lista. */
    val lastGroup: Map<String, String> = emptyMap(),
)

@Serializable
data class Settings(
    val sound: Boolean = true,
    val lastProfileId: String? = null,
    val onboarded: Boolean = false,
    /** Idioma de audio y de subtítulos que elegiste la última vez (código como "spa" o "es"; "off" sin subtítulos). */
    val audioLang: String = "",
    val subLang: String = "",
)

@Serializable
data class AppData(
    val profiles: List<Profile> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val profileData: Map<String, ProfileData> = emptyMap(),
    val settings: Settings = Settings(),
)

@Serializable
data class Programme(
    val start: Long,
    val stop: Long,
    val title: String,
    val desc: String = "",
)

fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
