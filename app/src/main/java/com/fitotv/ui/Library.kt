package com.fitotv.ui

import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.data.Fold

/** Películas o series con sus categorías, listas para pintar sin volver a recorrer la lista. */
class Catalog(val items: List<Channel>, val cats: List<String>, val byCat: Map<String, List<Channel>>) {
    companion object {
        val EMPTY = Catalog(emptyList(), emptyList(), emptyMap())

        fun of(items: List<Channel>): Catalog {
            val byCat = LinkedHashMap<String, MutableList<Channel>>()
            items.forEach { byCat.getOrPut(it.group) { ArrayList() }.add(it) }
            return Catalog(items, byCat.keys.toList(), byCat)
        }
    }
}

/**
 * Todo lo que las pantallas necesitan de la lista activa, calculado una sola vez fuera del
 * hilo de la pantalla. Antes cada pantalla recorría la lista completa (cientos de miles de
 * elementos en algunas listas) y la interfaz se trababa.
 */
class Library private constructor(
    /** Lo que el perfil puede ver (en perfiles infantiles, solo los grupos infantiles). */
    val all: List<Channel>,
    val byId: Map<String, Channel>,
    val live: List<Channel>,
    val groups: List<Group>,
    val liveByGroup: Map<String, List<Channel>>,
    val movies: Catalog,
    /** Una entrada por serie (no por episodio). */
    val shows: Catalog,
    /** Episodios de las series que llegaron sueltos en la lista M3U, ya en orden. */
    val episodesByShow: Map<String, List<Channel>>,
    private val searchItems: List<Channel>,
    private val searchKeys: Array<String>,
) {
    /** Hasta [limit] resultados; primero los que empiezan con lo que escribiste. */
    fun search(query: String, limit: Int = 60): List<Channel> {
        val q = Fold.fold(query.trim())
        if (q.length < 2) return emptyList()
        val starts = ArrayList<Channel>()
        val contains = ArrayList<Channel>()
        for (i in searchKeys.indices) {
            val k = searchKeys[i]
            if (k.startsWith(q)) {
                starts.add(searchItems[i])
                if (starts.size >= limit) break
            } else if (contains.size < limit && k.contains(q)) {
                contains.add(searchItems[i])
            }
        }
        return (starts + contains).take(limit)
    }

    companion object {
        val EMPTY = build(emptyList(), kids = false)

        fun build(raw: List<Channel>, kids: Boolean): Library {
            val visible = if (kids) raw.filter { Fold.isKidsGroup(it.group) } else raw
            val live = ArrayList<Channel>()
            val liveByGroup = LinkedHashMap<String, MutableList<Channel>>()
            val movies = ArrayList<Channel>()
            val shows = ArrayList<Channel>()
            val episodes = LinkedHashMap<String, MutableList<Channel>>()
            // Un id repetido haría que la pantalla se cierre (las listas de Compose piden claves únicas).
            val seen = HashSet<String>(visible.size * 2)
            for (ch in visible) {
                if (!seen.add(ch.id)) continue
                when (ch.kind) {
                    ContentKind.LIVE -> {
                        live.add(ch)
                        liveByGroup.getOrPut(ch.group) { ArrayList() }.add(ch)
                    }
                    ContentKind.MOVIE -> movies.add(ch)
                    ContentKind.SERIES -> when {
                        ch.isShow -> shows.add(ch)
                        ch.seriesId.isNotEmpty() -> {
                            val eps = episodes[ch.seriesId]
                            if (eps == null) {
                                episodes[ch.seriesId] = arrayListOf(ch)
                                // La serie toma el nombre, la imagen y la categoría de su primer episodio.
                                shows.add(
                                    Channel(
                                        id = "show:" + ch.seriesId,
                                        num = 0,
                                        name = ch.seriesId.removePrefix("m:"),
                                        url = "",
                                        logo = ch.logo,
                                        group = ch.group,
                                        kind = ContentKind.SERIES,
                                        seriesId = ch.seriesId,
                                    )
                                )
                            } else eps.add(ch)
                        }
                        else -> shows.add(ch)
                    }
                }
            }
            episodes.values.forEach { it.sortWith(compareBy<Channel>({ c -> c.season }, { c -> c.episode })) }
            val byId = HashMap<String, Channel>(visible.size + shows.size)
            visible.forEach { byId[it.id] = it }
            shows.forEach { byId[it.id] = it }
            val searchItems = ArrayList<Channel>(live.size + movies.size + shows.size)
            searchItems.addAll(live)
            searchItems.addAll(movies)
            searchItems.addAll(shows)
            val keys = Array(searchItems.size) { Fold.fold(searchItems[it].name) }
            return Library(
                all = visible,
                byId = byId,
                live = live,
                groups = liveByGroup.map { Group(it.key, it.value.size) },
                liveByGroup = liveByGroup,
                movies = Catalog.of(movies),
                shows = Catalog.of(shows),
                episodesByShow = episodes,
                searchItems = searchItems,
                searchKeys = keys,
            )
        }
    }
}
