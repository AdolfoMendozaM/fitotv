package com.fitotv.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.data.AppData
import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.data.Playlist
import com.fitotv.data.RecentItem
import com.fitotv.player.LivePreview
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.ChannelLogo
import com.fitotv.ui.components.EmptyState
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.MainScaffold
import com.fitotv.ui.components.Pill
import com.fitotv.ui.components.PrimaryButton
import com.fitotv.ui.components.ProgressBar
import com.fitotv.ui.components.RoundIconButton
import com.fitotv.ui.components.SectionTitle
import com.fitotv.ui.components.Spinner
import com.fitotv.ui.components.Tab
import com.fitotv.ui.rememberEntryFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T

/** Lo que se ve arriba: el elemento enfocado y su vista previa. */
sealed interface HeroItem {
    data class Vod(val ch: Channel, val recent: RecentItem?) : HeroItem
    data class Live(val ch: Channel) : HeroItem
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val list = vm.activeList(d)
    val profile = vm.profile(d)
    MainScaffold(vm, Tab.HOME) {
        when {
            list == null -> EmptyState(
                "Agrega tu primera lista",
                "Con un enlace, un archivo o el usuario y contraseña de tu proveedor.",
                "Agregar una lista",
                { vm.go(Screen.AddList(firstTime = false)) },
            )
            vm.channels.isEmpty() && vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(48.dp) }
            vm.channels.isEmpty() -> EmptyState(
                if (profile?.kids == true) "No hay canales infantiles aquí" else "Esta lista no tiene canales",
                if (profile?.kids == true) "Esta lista no trae un grupo Infantil. Prueba con otra lista." else "Revisa la lista o elige otra.",
                "Cambiar de lista",
                { vm.go(Screen.Lists) },
            )
            else -> HomeContent(vm, list, profile?.name ?: "", d)
        }
    }
}

@Composable
private fun HomeContent(vm: AppViewModel, list: Playlist, name: String, d: AppData) {
    val vodRecents = remember(d, vm.byId) { vm.recents(kindLive = false) }
    val liveRecents = remember(d, vm.byId) { vm.recents(kindLive = true) }
    val group = vm.selectedGroup
    val gridChannels = remember(vm.channels, group, d) { vm.channelsOf(group) }
    val rows = remember(gridChannels) { gridChannels.chunked(5) }
    val favorites = remember(d) { vm.profileData(d).favorites.toHashSet() }
    val groupTitle = if (group == AppViewModel.FAVORITES) "Favoritos" else group ?: "Canales"

    val fallback: HeroItem? = vodRecents.firstOrNull()?.let { HeroItem.Vod(it.first, it.second) }
        ?: liveRecents.firstOrNull()?.let { HeroItem.Live(it.first) }
        ?: gridChannels.firstOrNull()?.let { HeroItem.Live(it) }
    var hero by remember(list.id) { mutableStateOf<HeroItem?>(null) }
    var compact by remember { mutableStateOf(false) }
    val shown = hero ?: fallback
    val ef = rememberEntryFocus(vm, "home", list.id)
    val firstSection = when {
        vodRecents.isNotEmpty() -> 0
        liveRecents.isNotEmpty() -> 1
        else -> 3
    }

    fun playLive(ch: Channel, zap: List<Channel>) = vm.play(ch, if (zap.any { it.id == ch.id }) zap else listOf(ch))

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
    ) {
        Gap(h = 36.dp)
        Hero(
            vm = vm,
            item = shown,
            compact = compact,
            list = list,
            crumb = "Inicio, grupo $groupTitle",
            onPlay = { item ->
                when (item) {
                    is HeroItem.Vod -> vm.play(item.ch, listOf(item.ch))
                    is HeroItem.Live -> playLive(item.ch, gridChannels)
                }
            },
            onRestart = { item -> vm.play(item.ch, listOf(item.ch), fromStart = true) },
        )
        Gap(h = 14.dp)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (vodRecents.isNotEmpty()) {
                item(key = "vod") {
                    Column {
                        SectionTitle("Continuar viendo contenido de $name", "Lo último que viste")
                        Gap(h = 12.dp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            itemsIndexed(vodRecents, key = { _, p -> "v" + p.first.id }) { i, (ch, r) ->
                                VodCard(
                                    ch, r,
                                    onClick = {
                                        vm.markReturn("home", "v:" + ch.id)
                                        vm.play(ch, listOf(ch))
                                    },
                                    onFocus = { hero = HeroItem.Vod(ch, r); compact = false },
                                    focusRequester = ef.of("v:" + ch.id, i == 0 && firstSection == 0),
                                )
                            }
                            item(key = "todo") {
                                FocusBox(
                                    onClick = { vm.openTab(Screen.Catalog(ContentKind.MOVIE)) },
                                    onFocus = { compact = false },
                                    modifier = Modifier.size(205.dp, 150.dp),
                                    bg = Color.Transparent,
                                    focusedBg = Fc.S2,
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .border(2.dp, Fc.Line3, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Ico(Icons.HISTORY, 28.dp, Fc.Num)
                                            T("Ver todo", size = 15.sp, weight = FontWeight.SemiBold)
                                            T("Películas y series", size = 13.sp, color = Fc.Muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (liveRecents.isNotEmpty()) {
                item(key = "live") {
                    Column {
                        SectionTitle("Canales recientes", "Lo que está pasando ahora en los que visitaste")
                        Gap(h = 12.dp)
                        val zap = liveRecents.map { it.first }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            itemsIndexed(liveRecents, key = { _, p -> "l" + p.first.id }) { i, (ch, r) ->
                                RecentChannelCard(
                                    ch, vm.epgFor(ch).first, r.at,
                                    onClick = {
                                        vm.markReturn("home", "l:" + ch.id)
                                        playLive(ch, zap)
                                    },
                                    onFocus = { hero = HeroItem.Live(ch); compact = false },
                                    focusRequester = ef.of("l:" + ch.id, i == 0 && firstSection == 1),
                                )
                            }
                        }
                    }
                }
            }
            item(key = "groups") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (favorites.isNotEmpty()) {
                        item(key = "fav") {
                            GroupChip("Favoritos", favorites.size, group == AppViewModel.FAVORITES, star = true) {
                                vm.selectGroup(AppViewModel.FAVORITES)
                            }
                        }
                    }
                    items(vm.groups, key = { "g" + it.name }) { g ->
                        GroupChip(g.name, g.count, g.name == group) { vm.selectGroup(g.name) }
                    }
                    item(key = "all") {
                        FocusBox(
                            onClick = { vm.go(Screen.Groups) },
                            onFocus = { compact = false },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            radius = 22.dp,
                            bg = Color.Transparent,
                            focusedBg = Fc.S2,
                        ) {
                            Row(
                                Modifier
                                    .height(44.dp)
                                    .border(2.dp, Fc.Line2, RoundedCornerShape(22.dp))
                                    .padding(start = 12.dp, end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Ico(Icons.GRID, 20.dp, Fc.Muted2)
                                T("Todos los grupos", size = 16.sp, weight = FontWeight.SemiBold, color = Fc.Muted2)
                            }
                        }
                    }
                }
            }
            item(key = "gridtitle") {
                SectionTitle(groupTitle, if (gridChannels.size == 1) "1 canal" else "${gridChannels.size} canales")
            }
            items(rows.size, key = { "row$it" }) { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rows[r].forEachIndexed { i, ch ->
                        ChannelCard(
                            ch,
                            vm.epgFor(ch).first,
                            onClick = {
                                vm.markReturn("home", "g:" + ch.id)
                                playLive(ch, gridChannels)
                            },
                            // Sin recientes arriba, la primera fila de canales aún cabe con todo el encabezado.
                            onFocus = { hero = HeroItem.Live(ch); compact = r >= 1 || firstSection != 3 },
                            showGroup = false,
                            modifier = Modifier.weight(1f),
                            favorite = ch.id in favorites,
                            focusRequester = ef.of("g:" + ch.id, r == 0 && i == 0 && firstSection == 3),
                        )
                    }
                    repeat(5 - rows[r].size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun GroupChip(name: String, count: Int, selected: Boolean, star: Boolean = false, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        radius = 22.dp,
        bg = if (selected) Fc.Azul else Fc.S2,
        focusedBg = if (selected) Fc.Azul else Fc.S3,
    ) {
        Row(
            Modifier
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (star) Ico(Icons.STAR, 16.dp, if (selected) Color.White else Fc.Menta, fill = true)
            T(name, size = 16.sp, weight = FontWeight.SemiBold, color = if (selected) Color.White else Fc.Muted2)
            T(count.toString(), size = 14.sp, color = if (selected) Color(0xFFDCE4FF) else Fc.Muted)
        }
    }
}

@Composable
private fun Hero(
    vm: AppViewModel,
    item: HeroItem?,
    compact: Boolean,
    list: Playlist,
    crumb: String,
    onPlay: (HeroItem) -> Unit,
    onRestart: (HeroItem.Vod) -> Unit,
) {
    val h by animateDpAsState(if (compact) 180.dp else 220.dp, tween(420), label = "alto")
    val pw by animateDpAsState(if (compact) 320.dp else 391.dp, tween(420), label = "ancho")
    Row(
        Modifier
            .fillMaxWidth()
            .height(h),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (!compact) {
                FocusBox(
                    onClick = { vm.go(Screen.Lists) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    radius = 20.dp,
                ) {
                    Row(
                        Modifier
                            .height(40.dp)
                            .padding(start = 6.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Fc.Menta), contentAlignment = Alignment.Center) {
                            T(list.name.take(1).uppercase(), size = 14.sp, color = Fc.Noche, weight = FontWeight.Bold)
                        }
                        T(list.name, size = 15.sp, weight = FontWeight.SemiBold)
                        T("${vm.lib.live.size} canales", size = 15.sp, color = Fc.Muted)
                        Ico(Icons.CHEVRON_DOWN, 18.dp, Fc.Muted, stroke = 2.2f)
                    }
                }
                Gap(h = 14.dp)
            } else {
                Gap(h = 18.dp)
                T(crumb, size = 14.sp, color = Fc.Muted)
                Gap(h = 12.dp)
            }

            when (item) {
                is HeroItem.Vod -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pill(typeLabel(item.ch.kind))
                        T(item.ch.group, size = 16.sp, color = Fc.Muted2)
                    }
                    Gap(h = 8.dp)
                    T(item.ch.name, size = 36.sp, display = true, letterSpacing = (-0.5).sp)
                    Gap(h = 8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProgressBar(vodFraction(item.recent), 140.dp)
                        T(remaining(item.recent).replaceFirstChar { it.lowercase() }, size = 15.sp, color = Fc.Muted)
                    }
                }
                is HeroItem.Live -> {
                    val (now, next) = vm.epgFor(item.ch)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.ch.num > 0) T(item.ch.num.toString(), size = 30.sp, display = true, color = Fc.Num)
                        ChannelLogo(item.ch.name, item.ch.logo, width = 60.dp, height = 36.dp, corner = 9.dp, fontSize = 11f)
                        T(item.ch.name, size = 18.sp, weight = FontWeight.SemiBold)
                    }
                    Gap(h = 8.dp)
                    T(now?.title ?: "En vivo", size = 36.sp, display = true, letterSpacing = (-0.5).sp)
                    Gap(h = 8.dp)
                    if (now != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            T("Ahora, ${hhmm(now.start)} a ${hhmm(now.stop)}", size = 15.sp)
                            ProgressBar(progressOf(now), 140.dp)
                            T(minutesLeft(now), size = 15.sp, color = Fc.Muted)
                        }
                        if (!compact && next != null) {
                            Gap(h = 4.dp)
                            T("Después, ${next.title} a las ${hhmm(next.start)}", size = 15.sp, color = Fc.Muted)
                        }
                    } else {
                        T("Sin información de la guía para este canal", size = 15.sp, color = Fc.Muted)
                    }
                }
                null -> Unit
            }

            if (!compact && item != null) {
                Box(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (item) {
                        is HeroItem.Vod -> {
                            PrimaryButton("Continuar", onClick = { onPlay(item) }, icon = Icons.PLAY, height = 48.dp)
                            RoundIconButton(Icons.RESTART, onClick = { onRestart(item) }, size = 48.dp)
                        }
                        is HeroItem.Live -> {
                            PrimaryButton("Ver ahora", onClick = { onPlay(item) }, icon = Icons.PLAY, height = 48.dp)
                            val fav = vm.isFavorite(item.ch.id)
                            RoundIconButton(Icons.STAR, onClick = { vm.toggleFavorite(item.ch.id) }, size = 48.dp, fill = fav, tint = if (fav) Fc.Menta else Fc.Text)
                            RoundIconButton(Icons.GUIDE, onClick = { vm.openTab(Screen.Guide) }, size = 48.dp)
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .width(pw)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Fc.S3)
        ) {
            LivePreview((item as? HeroItem.Live)?.ch, Modifier.fillMaxSize())
            if (item is HeroItem.Vod) PosterArt(item.ch, Modifier.fillMaxSize(), fontSize = 56f)
            Row(
                Modifier
                    .padding(14.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xD90A1747))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Ico(if (item is HeroItem.Vod) Icons.FILM else Icons.SOUND, 16.dp, Fc.Text)
                T(if (item is HeroItem.Vod) typeLabel(item.ch.kind) else "Vista previa", size = 14.sp, weight = FontWeight.SemiBold)
            }
        }
    }
}
