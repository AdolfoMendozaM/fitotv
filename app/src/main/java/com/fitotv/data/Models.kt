package com.fitotv.data

import kotlinx.serialization.Serializable

/** Cómo se agregó la lista. */
@Serializable
enum class ListKind { URL, FILE, XTREAM }

/** Qué es cada elemento de la lista. */
@Serializable
enum class ContentKind { LIVE, MOVIE, SERIES }

/** Los cinco avatares que vienen con la app. */
enum class AvatarId(val key: String, val label: String) {
    AUDIFONOS("audifonos", "Avatar con audífonos"),
    PELO_LARGO("pelo_largo", "Avatar de pelo largo con aretes"),
    GORRA("gorra", "Avatar con gorra"),
    CHONGO("chongo", "Avatar con chongo y lentes"),
    COLETAS("coletas", "Avatar infantil con coletas");

    companion object {
        fun of(key: String?): AvatarId = entries.firstOrNull { it.key == key } ?: AUDIFONOS
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
)

@Serializable
data class RecentItem(
    val channelId: String,
    val listId: String,
    val kind: ContentKind,
    val at: Long,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
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
