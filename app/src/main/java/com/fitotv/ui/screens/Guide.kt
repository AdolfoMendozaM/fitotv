package com.fitotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.data.Channel
import com.fitotv.data.Programme
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
import com.fitotv.ui.components.ProgressBar
import com.fitotv.ui.components.Tab
import com.fitotv.ui.rememberAutoFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T

private const val SPAN_MIN = 120

@Composable
fun GuideScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    MainScaffold(vm, Tab.GUIDE) {
        val list = vm.activeList(d)
        val group = vm.selectedGroup
        val channels = remember(vm.channels, group, d) { vm.channelsOf(group) }
        if (list == null || channels.isEmpty()) {
            EmptyState("La guía está vacía", "Agrega una lista para ver qué están pasando.", "Agregar una lista", { vm.go(Screen.AddList(false)) }, Icons.GUIDE)
        } else {
            GuideContent(vm, channels, if (group == AppViewModel.FAVORITES) "Favoritos" else group ?: "Canales")
        }
    }
}

@Composable
private fun GuideContent(vm: AppViewModel, channels: List<Channel>, groupName: String) {
    val now = remember { System.currentTimeMillis() }
    val winStart = remember(now) { now - now % (30 * 60_000L) }
    val winEnd = winStart + SPAN_MIN * 60_000L
    var focusedCh by remember { mutableStateOf(channels.first()) }
    var focusedProg by remember { mutableStateOf<Programme?>(null) }
    val firstFr = rememberAutoFocus()

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 24.dp)
    ) {
        // Lo enfocado
        Row(Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (focusedCh.num > 0) T(focusedCh.num.toString(), size = 30.sp, display = true, color = Fc.Num)
                    ChannelLogo(focusedCh.name, focusedCh.logo, width = 64.dp, height = 38.dp, corner = 9.dp, fontSize = 12f)
                    T(focusedCh.name, size = 18.sp, weight = FontWeight.SemiBold)
                }
                Gap(h = 12.dp)
                val p = focusedProg
                T(p?.title ?: "Sin información de la guía", size = 32.sp, display = true, letterSpacing = (-0.5).sp)
                Gap(h = 8.dp)
                if (p != null) {
                    val t = System.currentTimeMillis()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        T("${hhmm(p.start)} a ${hhmm(p.stop)}", size = 15.sp)
                        if (p.start <= t && p.stop > t) {
                            ProgressBar(progressOf(p), 120.dp)
                            T("empezó hace ${(t - p.start) / 60000} min", size = 15.sp, color = Fc.Muted)
                        } else if (p.start > t) {
                            T("empieza en ${(p.start - t) / 60000} min", size = 15.sp, color = Fc.Muted)
                        }
                    }
                    if (p.desc.isNotBlank()) {
                        Gap(h = 10.dp)
                        T(p.desc, size = 15.sp, color = Fc.Muted2, maxLines = 2, lineHeight = 22.sp, modifier = Modifier.width(600.dp))
                    }
                } else {
                    T("Este canal no trae guía en tu lista. Presiona OK para verlo.", size = 15.sp, color = Fc.Muted)
                }
            }
            Box(
                Modifier
                    .size(320.dp, 180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Fc.S3)
            ) {
                LivePreview(focusedCh, Modifier.fillMaxSize())
                Row(
                    Modifier
                        .padding(12.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xD90A1747))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { T("Vista previa", size = 13.sp, weight = FontWeight.SemiBold) }
            }
        }
        Gap(h = 20.dp)

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val chCol = 290.dp
            val gap = 8.dp
            val timeline: Dp = maxWidth - chCol - gap
            fun x(t: Long): Dp = timeline * ((t - winStart).toFloat() / (winEnd - winStart)).coerceIn(0f, 1f)

            Column {
                // Encabezado: grupo y horas
                Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    FocusBox(
                        onClick = { vm.go(Screen.Groups) },
                        modifier = Modifier.size(chCol, 40.dp),
                        shape = RoundedCornerShape(20.dp),
                        radius = 20.dp,
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            T("Grupo", size = 15.sp, color = Fc.Muted)
                            T(groupName, size = 15.sp, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Ico(Icons.CHEVRON_DOWN, 18.dp, Fc.Muted, stroke = 2.2f)
                        }
                    }
                    Box(
                        Modifier
                            .padding(start = gap)
                            .width(timeline)
                            .fillMaxHeight()
                    ) {
                        for (m in 0 until SPAN_MIN step 30) {
                            val t = winStart + m * 60_000L
                            T((if (m == 0) "Hoy " else "") + hhmm(t), size = 15.sp, color = Fc.Muted, modifier = Modifier.offset(x = x(t)).align(Alignment.CenterStart))
                        }
                    }
                }
                Gap(h = 8.dp)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                ) {
                    itemsIndexed(channels, key = { _, c -> c.id }) { i, ch ->
                        val progs = remember(vm.epg, ch.id) {
                            if (ch.tvgId.isBlank()) emptyList()
                            else vm.epg[ch.tvgId.lowercase()].orEmpty().filter { it.stop > winStart && it.start < winEnd }
                        }
                        GuideRow(
                            ch = ch,
                            progs = progs,
                            chCol = chCol,
                            gap = gap,
                            timeline = timeline,
                            winStart = winStart,
                            winEnd = winEnd,
                            now = now,
                            x = ::x,
                            highlighted = ch.id == focusedCh.id,
                            firstFr = if (i == 0) firstFr else null,
                            onFocus = { p -> focusedCh = ch; focusedProg = p },
                            onPlay = { vm.play(ch, channels) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    ch: Channel,
    progs: List<Programme>,
    chCol: Dp,
    gap: Dp,
    timeline: Dp,
    winStart: Long,
    winEnd: Long,
    now: Long,
    x: (Long) -> Dp,
    highlighted: Boolean,
    firstFr: androidx.compose.ui.focus.FocusRequester?,
    onFocus: (Programme?) -> Unit,
    onPlay: () -> Unit,
) {
    Row(Modifier.height(64.dp)) {
        Row(
            Modifier
                .width(chCol)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(if (highlighted) Fc.S3 else Fc.S1)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            T(if (ch.num > 0) ch.num.toString() else "", size = 22.sp, display = true, color = Fc.Num, modifier = Modifier.width(40.dp))
            ChannelLogo(ch.name, ch.logo, width = 60.dp, height = 36.dp, corner = 8.dp, fontSize = 11f)
            T(ch.name, size = 14.sp, weight = FontWeight.SemiBold, maxLines = 2, lineHeight = 17.sp, modifier = Modifier.weight(1f))
        }
        Box(
            Modifier
                .padding(start = gap)
                .width(timeline)
                .fillMaxHeight()
        ) {
            Box(
                Modifier
                    .offset(x = x(now))
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Fc.Menta)
            )
            if (progs.isEmpty()) {
                GuideBlock(
                    title = "Sin información de la guía",
                    time = "Presiona OK para ver el canal",
                    left = 0.dp,
                    width = timeline,
                    past = false,
                    live = true,
                    focusRequester = firstFr,
                    onFocus = { onFocus(null) },
                    onClick = onPlay,
                )
            } else {
                var first = true
                progs.forEach { p ->
                    val l = x(p.start.coerceAtLeast(winStart))
                    val r = x(p.stop.coerceAtMost(winEnd))
                    val w = (r - l - 6.dp).coerceAtLeast(24.dp)
                    val live = p.start <= now && p.stop > now
                    GuideBlock(
                        title = p.title,
                        time = "${hhmm(p.start)} a ${hhmm(p.stop)}",
                        left = l,
                        width = w,
                        past = p.stop <= now,
                        live = live,
                        focusRequester = if (live && first) firstFr else null,
                        onFocus = { onFocus(p) },
                        onClick = onPlay,
                    )
                    if (live) first = false
                }
            }
        }
    }
}

@Composable
private fun GuideBlock(
    title: String,
    time: String,
    left: Dp,
    width: Dp,
    past: Boolean,
    live: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    FocusBox(
        onClick = onClick,
        onFocus = onFocus,
        focusRequester = focusRequester,
        modifier = Modifier
            .offset(x = left)
            .width(width)
            .fillMaxHeight(),
        radius = 14.dp,
        shape = RoundedCornerShape(14.dp),
        bg = when {
            past -> Fc.Noche
            live -> Fc.S3
            else -> Fc.S2
        },
        focusedBg = Fc.Azul,
    ) { f ->
        Column(
            Modifier
                .fillMaxSize()
                .then(if (past && !f) Modifier.border(1.dp, Fc.Track, RoundedCornerShape(14.dp)) else Modifier)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            T(title, size = 15.sp, weight = FontWeight.SemiBold, color = if (past && !f) Color(0xFF9AA7DA) else Color.White)
            T(time, size = 13.sp, color = if (f) Color(0xFFDCE4FF) else if (past) Fc.Dim else Fc.Muted)
        }
    }
}
