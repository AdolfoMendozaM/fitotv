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
            .padding(start = 96.dp, top = 56.dp, end = 96.dp)
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
                            modifier = Modifier.size(700.dp, 88.dp),
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
                        modifier = Modifier.size(700.dp, 88.dp),
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
    val groups = vm.groups
    val total = groups.sumOf { it.count }
    val firstFr = rememberAutoFocus()
    Column(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
            .padding(start = 96.dp, top = 56.dp, end = 96.dp)
    ) {
        T("Grupos de ${list?.name ?: ""}", size = 46.sp, display = true, letterSpacing = (-1).sp)
        Gap(h = 8.dp)
        T("$total canales en ${groups.size} grupos. Elige uno para ver sus canales.", size = 20.sp, color = Fc.Muted)
        Gap(h = 28.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
        ) {
            itemsIndexed(groups, key = { _, g -> g.name }) { i, g ->
                val selected = g.name == vm.selectedGroup
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
                            Ico(groupIcon(g.name), 24.dp, Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            T(g.name, size = 18.sp, weight = FontWeight.SemiBold)
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

@Composable
fun CatalogScreen(vm: AppViewModel, s: Screen.Catalog) {
    val d by vm.data.collectAsState()
    val tab = if (s.kind == ContentKind.MOVIE) Tab.MOVIES else Tab.SERIES
    val title = if (s.kind == ContentKind.MOVIE) "Películas" else "Series"
    val catalog = vm.catalog(s.kind)
    val all = catalog.items
    val cats = catalog.cats
    var cat by rememberSaveable(s.kind) { mutableStateOf<String?>(null) }
    val selectedCat = cat
    val shown = if (selectedCat == null) all else catalog.byCat[selectedCat].orEmpty()
    val recents = remember(d) { vm.profileData(d).recents.associateBy { it.channelId } }
    val ef = rememberEntryFocus(vm, "catalog-${s.kind}", s.kind)

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
                .padding(start = 48.dp, top = 36.dp, end = 48.dp)
        ) {
            SectionTitle(title, "${all.size} en ${vm.activeList(d)?.name ?: "tu lista"}")
            Gap(h = 12.dp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(8.dp)) {
                item(key = "todas") { CatChip("Todas", cat == null) { cat = null } }
                items(cats, key = { it }) { c -> CatChip(c, cat == c) { cat = c } }
            }
            Gap(h = 8.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
            ) {
                itemsIndexed(shown, key = { _, c -> c.id }) { i, ch ->
                    PosterCard(ch, recents[ch.id]?.let { vodFraction(it) } ?: 0f, ef.of(ch.id, i == 0)) {
                        vm.markReturn("catalog-${s.kind}", ch.id)
                        if (ch.isShow) vm.go(Screen.Series(ch.id)) else vm.play(ch, listOf(ch))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatChip(name: String, selected: Boolean, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        radius = 22.dp,
        bg = if (selected) Fc.Azul else Fc.S2,
        focusedBg = if (selected) Fc.Azul else Fc.S3,
    ) {
        Box(Modifier.height(44.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            T(name, size = 16.sp, weight = FontWeight.SemiBold, color = if (selected) Color.White else Fc.Muted2)
        }
    }
}

@Composable
private fun PosterCard(ch: Channel, progress: Float, fr: androidx.compose.ui.focus.FocusRequester?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusBox(onClick = onClick, focusRequester = fr, modifier = Modifier.fillMaxWidth().height(250.dp)) {
            PosterArt(ch, Modifier.fillMaxSize(), fontSize = 34f)
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
        T(ch.name, size = 15.sp, weight = FontWeight.SemiBold, maxLines = 2, lineHeight = 20.sp)
    }
}

// ---------------------------------------------------------------------------
// Buscar
// ---------------------------------------------------------------------------

@Composable
fun SearchScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Channel>>(emptyList()) }
    val ef = rememberEntryFocus(vm, "search")
    // La búsqueda corre fuera de la pantalla y espera a que dejes de escribir un momento.
    LaunchedEffect(query, vm.lib) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(180)
        results = vm.search(query)
    }
    MainScaffold(vm, Tab.SEARCH) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 48.dp, top = 36.dp, end = 48.dp)
        ) {
            T("Buscar", size = 40.sp, display = true)
            Gap(h = 16.dp)
            TvTextField(query, { query = it }, "Canal, película o serie", width = 640.dp, focusRequester = ef.first)
            Gap(h = 24.dp)
            when {
                query.trim().length < 2 -> T("Escribe al menos dos letras. Presiona OK para abrir el teclado.", size = 18.sp, color = Fc.Muted)
                results.isEmpty() -> T("No encontramos \"${query.trim()}\" en esta lista.", size = 18.sp, color = Fc.Muted)
                else -> {
                    SectionTitle("Resultados", "${results.size}")
                    Gap(h = 8.dp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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

@Composable
private fun SettingRow(
    title: String,
    sub: String,
    onClick: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
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
                T(sub, size = 15.sp, color = Fc.Muted)
            }
            trailing()
        }
    }
}
