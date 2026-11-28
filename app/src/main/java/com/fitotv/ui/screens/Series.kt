package com.fitotv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.fitotv.data.Channel
import com.fitotv.data.SeriesNames
import com.fitotv.data.XtreamApi
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.EmptyState
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.MainScaffold
import com.fitotv.ui.components.Spinner
import com.fitotv.ui.components.Tab
import com.fitotv.ui.rememberAutoFocus
import com.fitotv.ui.rememberEntryFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T

/**
 * Una serie: portada y descripción a la izquierda; temporadas y episodios a la derecha.
 * Los episodios de una temporada se ven en orden y el que dejaste a medias muestra cuánto llevas.
 */
@Composable
fun SeriesScreen(vm: AppViewModel, s: Screen.Series) {
    val d by vm.data.collectAsState()
    val show = remember(s.showId, vm.lib) { vm.channel(s.showId) }
    var info by remember(s.showId) { mutableStateOf<XtreamApi.SeriesInfo?>(null) }
    var error by remember(s.showId) { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(show?.id, attempt) {
        val sh = show ?: return@LaunchedEffect
        error = null
        vm.seriesInfo(sh)
            .onSuccess { info = it }
            .onFailure { error = it.message ?: "No pudimos abrir la serie" }
    }

    MainScaffold(vm, Tab.SERIES) {
        if (show == null) {
            EmptyState("Esta serie ya no está en la lista", "Puede que tu proveedor la haya quitado.", null, null, Icons.TV)
            return@MainScaffold
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 48.dp, top = 40.dp, end = 40.dp),
        ) {
            // Portada y datos
            Column(Modifier.width(250.dp)) {
                Box(
                    Modifier
                        .size(250.dp, 360.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    PosterArt(show.copy(logo = info?.cover?.ifBlank { show.logo } ?: show.logo), Modifier.fillMaxSize(), fontSize = 40f)
                }
                Gap(h = 18.dp)
                T(show.name, size = 26.sp, display = true, maxLines = 3, lineHeight = 30.sp)
                Gap(h = 8.dp)
                val seasons = info?.seasons.orEmpty()
                val eps = seasons.sumOf { it.episodes.size }
                val parts = buildList {
                    info?.year?.takeIf { it.isNotBlank() }?.let { add(it) }
                    if (seasons.isNotEmpty()) add(if (seasons.size == 1) "1 temporada" else "${seasons.size} temporadas")
                    if (eps > 0) add(if (eps == 1) "1 episodio" else "$eps episodios")
                }
                if (parts.isNotEmpty()) T(parts.joinToString(", "), size = 15.sp, color = Fc.Muted)
                info?.plot?.takeIf { it.isNotBlank() }?.let {
                    Gap(h = 12.dp)
                    T(it, size = 14.sp, color = Fc.Muted2, maxLines = 8, lineHeight = 20.sp)
                }
            }
            Gap(w = 36.dp)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val i = info
                when {
                    error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        T("No pudimos abrir la serie", size = 26.sp, display = true)
                        Gap(h = 8.dp)
                        T(error.orEmpty(), size = 16.sp, color = Fc.Muted, maxLines = 3)
                        Gap(h = 20.dp)
                        val fr = rememberAutoFocus(attempt)
                        FocusBox(onClick = { attempt += 1 }, focusRequester = fr, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(26.dp), radius = 26.dp, bg = Fc.Azul, focusedBg = Fc.Azul) {
                            Row(Modifier.height(52.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Ico(Icons.REFRESH, 20.dp, Color.White)
                                Gap(w = 10.dp)
                                T("Intentar otra vez", size = 17.sp, weight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    i == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spinner(44.dp)
                        Gap(h = 14.dp)
                        T("Cargando temporadas…", size = 16.sp, color = Fc.Muted)
                    }
                    i.seasons.isEmpty() -> EmptyState("Esta serie todavía no tiene episodios", "Tu proveedor no mandó ninguno. Prueba más tarde.", null, null, Icons.TV)
                    else -> SeasonsAndEpisodes(vm, show, i, vm.profileData(d).recents)
                }
            }
        }
    }
}

@Composable
private fun SeasonsAndEpisodes(vm: AppViewModel, show: Channel, info: XtreamApi.SeriesInfo, recents: List<com.fitotv.data.RecentItem>) {
    val progress = remember(recents) { recents.associateBy { it.channelId } }
    // Empieza en la temporada del último episodio que viste.
    val startSeason = remember(info) {
        val last = recents.firstOrNull { r -> info.seasons.any { s -> s.episodes.any { it.id == r.channelId } } }
        info.seasons.indexOfFirst { s -> s.episodes.any { it.id == last?.channelId } }.coerceAtLeast(0)
    }
    var season by remember(info) { mutableIntStateOf(startSeason) }
    val current = info.seasons[season.coerceIn(0, info.seasons.lastIndex)]
    val resumeId = remember(current, recents) { current.episodes.firstOrNull { progress[it.id] != null }?.id }
    val ef = rememberEntryFocus(vm, "series-${show.id}", show.id)

    Column(Modifier.fillMaxSize()) {
        if (info.seasons.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(8.dp)) {
                itemsIndexed(info.seasons, key = { _, s -> s.number }) { idx, s ->
                    val selected = idx == season
                    FocusBox(
                        onClick = { season = idx },
                        onFocus = { season = idx },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        radius = 22.dp,
                        bg = if (selected) Fc.Azul else Fc.S2,
                        focusedBg = if (selected) Fc.Azul else Fc.S3,
                    ) {
                        Box(Modifier.height(44.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                            T(s.name, size = 16.sp, weight = FontWeight.SemiBold, color = if (selected) Color.White else Fc.Muted2)
                        }
                    }
                }
            }
            Gap(h = 10.dp)
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 40.dp),
        ) {
            itemsIndexed(current.episodes, key = { _, e -> e.id }) { idx, ep ->
                val r = progress[ep.id]
                EpisodeRow(
                    ep = ep,
                    fraction = vodFraction(r),
                    focusRequester = ef.of(ep.id, (resumeId == null && idx == 0) || ep.id == resumeId),
                    onClick = {
                        vm.markReturn("series-${show.id}", ep.id)
                        vm.play(ep, current.episodes)
                    },
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(ep: Channel, fraction: Float, focusRequester: androidx.compose.ui.focus.FocusRequester?, onClick: () -> Unit) {
    val title = remember(ep.name) {
        SeriesNames.parse(ep.name)?.title?.takeIf { it.isNotBlank() } ?: ep.name.takeIf { !it.startsWith(ep.group) } ?: ""
    }
    FocusBox(
        onClick = onClick,
        focusRequester = focusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        bg = Fc.S1,
        focusedBg = Fc.S3,
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(136.dp, 76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Fc.S3)
            ) {
                PosterArt(ep, Modifier.fillMaxSize(), fontSize = 20f)
                if (fraction > 0f) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(Color(0xB30A1747))) {
                        Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(Fc.Menta))
                    }
                }
            }
            Gap(w = 18.dp)
            Column(Modifier.weight(1f)) {
                T(if (ep.episode > 0) "Episodio ${ep.episode}" else "Episodio", size = 14.sp, color = Fc.Num, weight = FontWeight.SemiBold)
                if (title.isNotBlank()) T(title, size = 18.sp, weight = FontWeight.SemiBold, maxLines = 1)
            }
            Ico(Icons.PLAY, 22.dp, Fc.Muted, fill = true)
            Gap(w = 10.dp)
        }
    }
}
