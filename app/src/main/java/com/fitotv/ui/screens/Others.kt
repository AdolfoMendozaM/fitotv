package com.fitotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.BuildConfig
import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.data.ListKind
import com.fitotv.data.Playlist
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.EmptyState
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.MainScaffold
import com.fitotv.ui.components.Pill
import com.fitotv.ui.components.SectionTitle
import com.fitotv.ui.components.Switch
import com.fitotv.ui.components.Tab
import com.fitotv.ui.components.TvTextField
import com.fitotv.ui.rememberAutoFocus
import com.fitotv.ui.rememberEntryFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Listas de un perfil
// ---------------------------------------------------------------------------

@Composable
fun ListsScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val profile = vm.profile(d)
    val lists = vm.listsOf(d)
    val active = vm.activeList(d)
    val firstFr = rememberAutoFocus()
    var confirm by remember { mutableStateOf<String?>(null) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
            .padding(start = 72.dp, top = 56.dp, end = 48.dp)
    ) {
        Column {
            T("Listas de ${profile?.name ?: ""}", size = 46.sp, display = true, letterSpacing = (-1).sp)
            Gap(h = 8.dp)
            T("Elige cuál quieres ver. Cada perfil tiene las suyas.", size = 20.sp, color = Fc.Muted)
            Gap(h = 32.dp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)) {
                itemsIndexed(lists, key = { _, l -> l.id }) { i, l ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FocusBox(
                            onClick = {
                                if (l.id != active?.id) vm.setActiveList(l.id)
                                vm.back()
                            },
                            modifier = Modifier.size(600.dp, 88.dp),
                            focusRequester = if (l.id == active?.id || (active == null && i == 0)) firstFr else null,
                            bg = Fc.S1,
                            focusedBg = Fc.S3,
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Fc.Menta), contentAlignment = Alignment.Center) {
                                    T(l.name.take(1).uppercase(), size = 22.sp, color = Fc.Noche, weight = FontWeight.ExtraBold)
                                }
                                Column(Modifier.weight(1f)) {
                                    T(l.name, size = 20.sp, weight = FontWeight.SemiBold)
                                    T(listSubtitle(l), size = 15.sp, color = if (l.lastError != null) Fc.Ambar else Fc.Muted)
                                }
                                if (l.id == active?.id) Pill("Viendo")
                            }
                        }
                        SmallAction(Icons.EDIT, "Editar") { vm.go(Screen.AddList(firstTime = false, editId = l.id)) }
                        SmallAction(Icons.REFRESH, "Actualizar") {
                            if (l.id != active?.id) vm.setActiveList(l.id) else vm.refreshActive(quietWhenSame = false)
                        }
                        SmallAction(Icons.TRASH, if (confirm == l.id) "¿Seguro? Otra vez" else "Quitar") {
                            if (confirm == l.id) {
                                vm.removeList(l.id)
                                confirm = null
                            } else confirm = l.id
                        }
                    }
                }
                item(key = "agregar") {
                    FocusBox(
                        onClick = { vm.go(Screen.AddList(firstTime = false)) },
                        modifier = Modifier.size(600.dp, 88.dp),
                        focusRequester = if (lists.isEmpty()) firstFr else null,
                        bg = Color.Transparent,
                        focusedBg = Fc.S2,
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .border(2.dp, Fc.Line3, RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Fc.S2), contentAlignment = Alignment.Center) {
                                Ico(Icons.PLUS, 26.dp, Fc.Num)
                            }
                            Column {
                                T("Agregar una lista", size = 20.sp, weight = FontWeight.SemiBold)
                                T("Desde tu celular con el QR, sin escribir con el control", size = 15.sp, color = Fc.Muted)
                            }
                        }
                    }
                }
            }
        }
        T(
            "Presiona Atrás para cerrar",
            size = 15.sp,
            color = Fc.Muted,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp),
        )
    }
}

private fun listSubtitle(l: Playlist): String {
    if (l.lastError != null) return "No se pudo actualizar: ${l.lastError}"
    val kind = when (l.kind) {
        ListKind.URL -> "Enlace"
        ListKind.FILE -> "Archivo"
        ListKind.XTREAM -> "Usuario y contraseña"
    }
    val updated = if (l.updatedAt > 0) ", actualizada " + SimpleDateFormat("d MMM, HH:mm", Locale("es", "MX")).format(Date(l.updatedAt)) else ""
    return "${l.channelCount} canales, $kind$updated"
}

@Composable
private fun SmallAction(icon: String, label: String, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        radius = 28.dp,
    ) {
        Row(
            Modifier
                .height(56.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Ico(icon, 20.dp, Fc.Muted2)
            T(label, size = 16.sp, weight = FontWeight.SemiBold, color = Fc.Muted2)
        }
    }
}

// ---------------------------------------------------------------------------
// Grupos
// ---------------------------------------------------------------------------

fun groupIcon(name: String): String {
    val g = AppViewModel.fold(name)
    return when {
        listOf("notici", "news", "informa").any { g.contains(it) } -> Icons.NEWS
        listOf("deport", "sport", "futbol", "liga", "box", "nfl", "nba").any { g.contains(it) } -> Icons.BALL
        AppViewModel.isKidsGroup(name) -> Icons.KIDS
        listOf("cine", "pelic", "movie", "film").any { g.contains(it) } -> Icons.FILM
        listOf("musica", "music", "radio").any { g.contains(it) } -> Icons.MUSIC
        listOf("document", "cultura", "educa").any { g.contains(it) } -> Icons.BOOK
        listOf("mexico", "local", "nacional", "abierta").any { g.contains(it) } -> Icons.PIN
        listOf("usa", "latino", "espana", "internacional", "world", "argentina", "colombia").any { g.contains(it) } -> Icons.GLOBE
        else -> Icons.TV
    }
}

@Composable
fun GroupsScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val list = vm.activeList(d)
    val favCount = remember(d) { vm.channelsOf(AppViewModel.FAVORITES).size }
    val groups = if (favCount > 0) listOf(com.fitotv.ui.Group(AppViewModel.FAVORITES, favCount)) + vm.groups else vm.groups
    val total = vm.groups.sumOf { it.count }
    val firstFr = rememberAutoFocus()
    Column(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
            .padding(start = 96.dp, top = 56.dp, end = 96.dp)
    ) {
        T("Grupos de ${list?.name ?: ""}", size = 46.sp, display = true, letterSpacing = (-1).sp)
        Gap(h = 8.dp)
        T("$total canales en ${vm.groups.size} grupos. Elige uno para ver sus canales.", size = 20.sp, color = Fc.Muted)
        Gap(h = 28.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
        ) {
            itemsIndexed(groups, key = { _, g -> g.name }) { i, g ->
                val selected = g.name == vm.selectedGroup
                val fav = g.name == AppViewModel.FAVORITES
                val label = if (fav) "Favoritos" else g.name
                FocusBox(
                    onClick = {
                        vm.selectGroup(g.name)
                        vm.back()
                    },
                    modifier = Modifier.height(96.dp),
                    focusRequester = if (selected || (vm.selectedGroup == null && i == 0)) firstFr else null,
                    bg = if (selected) Fc.S3 else Fc.S2,
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) Fc.Azul else Fc.S3), contentAlignment = Alignment.Center) {
                            Ico(if (fav) Icons.STAR else groupIcon(g.name), 24.dp, if (fav) Fc.Menta else Color.White, fill = fav)
                        }
                        Column(Modifier.weight(1f)) {
                            T(label, size = 18.sp, weight = FontWeight.SemiBold, maxLines = 2, lineHeight = 22.sp)
                            T(if (g.count == 1) "1 canal" else "${g.count} canales", size = 14.sp, color = Fc.Muted)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Películas y series
// ---------------------------------------------------------------------------

/**
 * Películas o series como en Netflix: arriba las categorías para brincar a una; abajo filas
 * con lo que dejaste a medias, lo más nuevo y una fila por categoría. Al elegir una categoría
 * se ve completa en cuadrícula.
 */
@Composable
fun CatalogScreen(vm: AppViewModel, s: Screen.Catalog) {
    val d by vm.data.collectAsState()
    val tab = if (s.kind == ContentKind.MOVIE) Tab.MOVIES else Tab.SERIES
    val title = if (s.kind == ContentKind.MOVIE) "Películas" else "Series"
    val catalog = vm.catalog(s.kind)
    val all = catalog.items
    val cats = catalog.cats
    var cat by rememberSaveable(s.kind) { mutableStateOf<String?>(null) }
    // Al elegir una categoría el foco baja a sus portadas (al regresar de una película no).
    var jump by remember { mutableStateOf(false) }
    fun pick(c: String?) {
        cat = c
        jump = c != null
    }
    val recents = remember(d) { vm.profileData(d).recents.associateBy { it.channelId } }
    val continueItems = remember(d, vm.byId, s.kind) {
        vm.recents(kindLive = false).filter { (ch, _) -> ch.kind == s.kind }
    }
    val key = "catalog-${s.kind}"
    val ef = rememberEntryFocus(vm, key, s.kind)

    fun open(ch: Channel, itemKey: String) {
        vm.markReturn(key, itemKey)
        if (ch.isShow) vm.go(Screen.Series(ch.id)) else vm.play(ch, listOf(ch))
    }

    MainScaffold(vm, tab) {
        if (all.isEmpty()) {
            EmptyState(
                "Tu lista no trae $title".lowercase().replaceFirstChar { it.uppercase() },
                "Algunas listas solo traen canales en vivo. Si tu proveedor te dio otra lista con ${title.lowercase()}, agrégala.",
                null, null, if (s.kind == ContentKind.MOVIE) Icons.FILM else Icons.TV,
            )
            return@MainScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 52.dp, top = 36.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                T(title, size = 40.sp, display = true, letterSpacing = (-0.5).sp)
                T("${all.size} en ${vm.activeList(d)?.name ?: "tu lista"}", size = 16.sp, color = Fc.Muted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Gap(h = 14.dp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 4.dp, top = 8.dp, end = 56.dp, bottom = 8.dp),
            ) {
                item(key = "inicio") { CatChip("Todo", cat == null) { pick(null) } }
                items(cats, key = { it }) { c -> CatChip(c, cat == c) { pick(c) } }
            }
            Gap(h = 10.dp)
            val selected = cat
            if (selected == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                ) {
                    var firstUsed = false
                    fun isFirst(): Boolean = if (!firstUsed) { firstUsed = true; true } else false
                    if (continueItems.isNotEmpty()) {
                        val first = isFirst()
                        item(key = "continua") {
                            PosterRow("Continúa", null) {
                                itemsIndexed(continueItems, key = { _, p -> "c" + p.first.id }) { i, (ch, r) ->
                                    PosterCard(ch, vodFraction(r), ef.of("c:" + ch.id, first && i == 0)) { open(ch, "c:" + ch.id) }
                                }
                            }
                        }
                    }
                    val newest = all.take(24)
                    val firstNew = isFirst()
                    item(key = "nuevo") {
                        PosterRow("Lo más nuevo", null) {
                            itemsIndexed(newest, key = { _, c -> "n" + c.id }) { i, ch ->
                                PosterCard(ch, recents[ch.id]?.let { vodFraction(it) } ?: 0f, ef.of("n:" + ch.id, firstNew && i == 0)) { open(ch, "n:" + ch.id) }
                            }
                        }
                    }
                    items(cats, key = { "row:$it" }) { c ->
                        val inCat = catalog.byCat[c].orEmpty()
                        PosterRow(c, if (inCat.size == 1) "1 título" else "${inCat.size} títulos") {
                            itemsIndexed(inCat.take(20), key = { _, ch -> "r" + ch.id }) { _, ch ->
                                PosterCard(ch, recents[ch.id]?.let { vodFraction(it) } ?: 0f, ef.of("r:$c:" + ch.id, false)) { open(ch, "r:$c:" + ch.id) }
                            }
                            if (inCat.size > 20) {
                                item(key = "todo") { SeeAllCard(inCat.size) { pick(c) } }
                            }
                        }
                    }
                }
            } else {
                val shown = catalog.byCat[selected].orEmpty()
                val gridFirst = remember { androidx.compose.ui.focus.FocusRequester() }
                LaunchedEffect(selected, jump) {
                    if (jump) {
                        kotlinx.coroutines.delay(80)
                        runCatching { gridFirst.requestFocus() }
                        jump = false
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    T(selected, size = 24.sp, display = true)
                    T(if (shown.size == 1) "1 título" else "${shown.size} títulos", size = 15.sp, color = Fc.Muted, modifier = Modifier.padding(bottom = 3.dp))
                }
                Gap(h = 10.dp)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(start = 6.dp, top = 8.dp, end = 52.dp, bottom = 48.dp),
                ) {
                    itemsIndexed(shown, key = { _, c -> c.id }) { i, ch ->
                        val back = ef.of("g:" + ch.id, false)
                        PosterCard(ch, recents[ch.id]?.let { vodFraction(it) } ?: 0f, back ?: if (i == 0) gridFirst else null) { open(ch, "g:" + ch.id) }
                    }
                }
            }
        }
    }
}

/** Una fila de portadas con su título, como en Netflix. */
@Composable
private fun PosterRow(title: String, sub: String?, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 4.dp)) {
            T(title, size = 21.sp, display = true)
            if (sub != null) T(sub, size = 14.sp, color = Fc.Muted, modifier = Modifier.padding(bottom = 2.dp))
        }
        Gap(h = 10.dp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 56.dp, bottom = 6.dp),
            content = content,
        )
    }
}

@Composable
private fun SeeAllCard(count: Int, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.size(140.dp, 210.dp),
        bg = Color.Transparent,
        focusedBg = Fc.S2,
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().border(2.dp, Fc.Line3, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Ico(Icons.GRID, 26.dp, Fc.Num)
                T("Ver todo", size = 15.sp, weight = FontWeight.SemiBold)
                T("$count títulos", size = 13.sp, color = Fc.Muted)
            }
        }
    }
}

@Composable
private fun CatChip(name: String, selected: Boolean, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        radius = 12.dp,
        bg = if (selected) Color.White else Fc.S2,
        focusedBg = if (selected) Color.White else Fc.S3,
    ) {
        Box(Modifier.height(44.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            T(name, size = 16.sp, weight = FontWeight.Bold, color = if (selected) Fc.Noche else Fc.Muted2)
        }
    }
}

/** Portada vertical con el nombre abajo y, si la dejaste a medias, la barrita de avance. */
@Composable
private fun PosterCard(ch: Channel, progress: Float, fr: androidx.compose.ui.focus.FocusRequester?, onClick: () -> Unit) {
    Column(Modifier.width(140.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusBox(onClick = onClick, focusRequester = fr, modifier = Modifier.size(140.dp, 210.dp), radius = 12.dp, shape = RoundedCornerShape(12.dp)) {
            PosterArt(ch, Modifier.fillMaxSize(), fontSize = 30f)
            if (progress > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Color(0xB30A1747))
                ) {
                    Box(Modifier.fillMaxWidth(progress).height(5.dp).background(Fc.Menta))
                }
            }
        }
        T(ch.name, size = 14.sp, weight = FontWeight.SemiBold, maxLines = 1, color = Fc.Muted2)
    }
}

// ---------------------------------------------------------------------------
// Buscar
// ---------------------------------------------------------------------------

private val searchKinds = listOf<Pair<String, ContentKind?>>(
    "Todo" to null,
    "Canales" to ContentKind.LIVE,
    "Películas" to ContentKind.MOVIE,
    "Series" to ContentKind.SERIES,
)

@Composable
fun SearchScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var kindIdx by rememberSaveable { mutableStateOf(0) }
    var results by remember { mutableStateOf<List<Channel>>(emptyList()) }
    val ef = rememberEntryFocus(vm, "search")
    val kind = searchKinds[kindIdx].second
    // La búsqueda corre fuera de la pantalla y espera a que dejes de escribir un momento.
    LaunchedEffect(query, kindIdx, vm.lib) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(180)
        results = vm.search(query, kind)
    }
    MainScaffold(vm, Tab.SEARCH) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 52.dp, top = 36.dp, end = 52.dp)
        ) {
            T("Buscar", size = 40.sp, display = true)
            Gap(h = 16.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TvTextField(query, { query = it }, "Canal, película o serie", width = 520.dp, focusRequester = ef.first)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    searchKinds.forEachIndexed { i, (label, _) ->
                        CatChip(label, i == kindIdx) { kindIdx = i }
                    }
                }
            }
            Gap(h = 24.dp)
            val what = when (kind) {
                ContentKind.LIVE -> "canales"
                ContentKind.MOVIE -> "películas"
                ContentKind.SERIES -> "series"
                null -> "resultados"
            }
            when {
                query.trim().length < 2 -> T("Escribe al menos dos letras. Presiona OK para abrir el teclado y elige si buscas canales, películas o series.", size = 18.sp, color = Fc.Muted, maxLines = 2, lineHeight = 26.sp)
                results.isEmpty() -> T("No encontramos $what con \"${query.trim()}\" en esta lista.", size = 18.sp, color = Fc.Muted)
                else -> {
                    SectionTitle(what.replaceFirstChar { it.uppercase() }, "${results.size}")
                    Gap(h = 8.dp)
                    if (kind == ContentKind.MOVIE || kind == ContentKind.SERIES) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(140.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
                        ) {
                            itemsIndexed(results, key = { _, c -> c.id }) { _, ch ->
                                PosterCard(ch, 0f, ef.of(ch.id, false)) {
                                    vm.markReturn("search", ch.id)
                                    if (ch.isShow) vm.go(Screen.Series(ch.id)) else vm.play(ch, listOf(ch))
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
                        ) {
                            itemsIndexed(results, key = { _, c -> c.id }) { _, ch ->
                                ChannelCard(
                                    ch,
                                    if (ch.kind == ContentKind.LIVE) vm.epgFor(ch).first else null,
                                    focusRequester = ef.of(ch.id, false),
                                    onClick = {
                                        vm.markReturn("search", ch.id)
                                        when {
                                            ch.isShow -> vm.go(Screen.Series(ch.id))
                                            ch.kind == ContentKind.LIVE -> vm.play(ch, results.filter { it.kind == ContentKind.LIVE })
                                            else -> vm.play(ch, listOf(ch))
                                        }
                                    },
                                    onFocus = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ajustes
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val profile = vm.profile(d)
    val list = vm.activeList(d)
    val firstFr = rememberAutoFocus()
    var about by remember { mutableStateOf(false) }
    LaunchedEffect(list?.id) { vm.refreshAccount() }
    MainScaffold(vm, Tab.SETTINGS) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 64.dp, top = 48.dp, end = 64.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            T("Ajustes", size = 46.sp, display = true, letterSpacing = (-1).sp)
            Gap(h = 10.dp)
            val upd = vm.update
            if (upd != null) {
                val prog = vm.updateProgress
                SettingRow(
                    "Actualizar FitoTV a la ${upd.versionName}",
                    when {
                        prog != null -> "Descargando… ${(prog * 100).toInt()} %"
                        upd.notes.isNotBlank() -> upd.notes
                        else -> "Tienes la ${BuildConfig.VERSION_NAME}. Se instala encima sin borrar perfiles ni listas."
                    },
                    focusRequester = firstFr,
                    onClick = { vm.installUpdate() },
                ) {
                    Ico(Icons.REFRESH, 24.dp, Fc.Menta)
                }
                vm.updateMsg?.let { msg ->
                    T(msg, size = 16.sp, color = Fc.Ambar, maxLines = 3, lineHeight = 23.sp, modifier = Modifier.width(820.dp))
                }
            }
            SettingRow("Sonidos", "Los sonidos de marimba al moverte y al abrir FitoTV", focusRequester = if (upd == null) firstFr else null, onClick = { vm.setSound(!d.settings.sound) }) {
                Switch(d.settings.sound)
            }
            SettingRow("Mis listas", "${vm.listsOf(d).size} de ${profile?.name ?: "este perfil"}", onClick = { vm.go(Screen.Lists) }) {
                T("Abrir", size = 16.sp, color = Fc.Muted)
            }
            if (list != null && vm.hasAccountInfo(list)) {
                val (sub, warn) = accountText(list)
                SettingRow("Vigencia de tu cuenta de ${list.name}", sub, onClick = { vm.refreshAccount(force = true) }, subColor = if (warn) Fc.Ambar else Fc.Muted) {
                    Ico(Icons.CLOCK, 24.dp, if (warn) Fc.Ambar else Fc.Menta)
                }
            }
            if (list != null) {
                SettingRow("Actualizar ${list.name} ahora", "Se actualiza sola en segundo plano varias veces al día", onClick = { vm.refreshActive(quietWhenSame = false) }) {
                    Ico(Icons.REFRESH, 24.dp, Fc.Muted2)
                }
            }
            SettingRow("Cambiar de perfil", "Ahora: ${profile?.name ?: ""}", onClick = { vm.go(Screen.Profiles) }) {
                Ico(Icons.USER, 24.dp, Fc.Muted2)
            }
            SettingRow(
                "Acerca de FitoTV",
                if (BuildConfig.SELF_UPDATE) "Versión ${BuildConfig.VERSION_NAME} · OK para buscar actualizaciones"
                else "Versión ${BuildConfig.VERSION_NAME} · Se actualiza sola desde la tienda de Amazon",
                onClick = { about = true; vm.checkUpdate(manual = true) },
            ) {
                Ico(Icons.INFO, 24.dp, Fc.Muted2)
            }
            if (about) {
                T(
                    "FitoTV es solo un reproductor: no incluye canales. Reproduce las listas que tú agregas, de proveedores que tengas permiso de usar.",
                    size = 16.sp, color = Fc.Muted, maxLines = 3, lineHeight = 23.sp, modifier = Modifier.width(820.dp),
                )
            }
        }
    }
}

/** "Vence el 12 de enero de 2027 · faltan 45 días · 2 pantallas a la vez" y si hay que avisar. */
private fun accountText(l: Playlist, now: Long = System.currentTimeMillis()): Pair<String, Boolean> {
    if (l.accountCheckedAt == 0L) return "Preguntando a tu proveedor…" to false
    val screens = when {
        l.maxScreens == 1 -> " · 1 pantalla a la vez"
        l.maxScreens > 1 -> " · ${l.maxScreens} pantallas a la vez"
        else -> ""
    }
    val trial = if (l.trial) " · Cuenta de prueba" else ""
    val status = l.accountStatus.lowercase()
    if (status == "banned" || status == "disabled") return "Tu proveedor desactivó la cuenta" to true
    if (l.expiresAt < 0) return "Sin fecha de vencimiento$screens$trial" to false
    if (l.expiresAt == 0L) return "Tu proveedor no dice cuándo vence$screens" to false
    val date = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date(l.expiresAt))
    val days = ((l.expiresAt - now) / 86_400_000L).toInt()
    return when {
        l.expiresAt <= now || status == "expired" -> "Venció el $date. Renuévala con tu proveedor$screens" to true
        days == 0 -> "Vence hoy, $date$screens$trial" to true
        days == 1 -> "Vence mañana, $date$screens$trial" to true
        else -> "Vence el $date · faltan $days días$screens$trial" to (days <= 7)
    }
}

@Composable
private fun SettingRow(
    title: String,
    sub: String,
    onClick: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    subColor: Color = Fc.Muted,
    trailing: @Composable () -> Unit,
) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.size(820.dp, 84.dp),
        focusRequester = focusRequester,
        bg = Fc.S1,
        focusedBg = Fc.S3,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                T(title, size = 19.sp, weight = FontWeight.SemiBold)
                T(sub, size = 15.sp, color = subColor)
            }
            trailing()
        }
    }
}
