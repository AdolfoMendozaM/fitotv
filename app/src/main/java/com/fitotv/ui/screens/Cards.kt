package com.fitotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.data.EpgRepository
import com.fitotv.data.Programme
import com.fitotv.data.RecentItem
import com.fitotv.ui.components.ChannelLogo
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.initials
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ----- Textos de tiempo -----

private val hm = SimpleDateFormat("HH:mm", Locale.US)

fun hhmm(t: Long): String = hm.format(Date(t))

fun minutesLeft(p: Programme, now: Long = System.currentTimeMillis()): String {
    val m = ((p.stop - now) / 60000L).coerceAtLeast(0)
    return if (m >= 60) "quedan ${m / 60} h ${m % 60} min" else "quedan $m min"
}

fun progressOf(p: Programme?, now: Long = System.currentTimeMillis()): Float {
    if (p == null || p.stop <= p.start) return 0f
    return ((now - p.start).toFloat() / (p.stop - p.start)).coerceIn(0f, 1f)
}

fun visitedAgo(at: Long, now: Long = System.currentTimeMillis()): String {
    val min = (now - at) / 60000L
    return when {
        min < 2 -> "Visitado hace un momento"
        min < 60 -> "Visitado hace $min min"
        min < 24 * 60 -> "Visitado hace ${min / 60} h"
        min < 48 * 60 -> "Visitado ayer"
        else -> "Visitado hace ${min / (24 * 60)} días"
    }
}

fun remaining(r: RecentItem?): String {
    if (r == null || r.durationMs <= 0) return "Sin empezar"
    val left = ((r.durationMs - r.positionMs) / 60000L).coerceAtLeast(1)
    return if (left >= 60) "Quedan ${left / 60} h ${"%02d".format(left % 60)} min" else "Quedan $left min"
}

fun vodFraction(r: RecentItem?): Float =
    if (r == null || r.durationMs <= 0) 0f else (r.positionMs.toFloat() / r.durationMs).coerceIn(0f, 1f)

fun typeLabel(kind: ContentKind) = when (kind) {
    ContentKind.MOVIE -> "Película"
    ContentKind.SERIES -> "Serie"
    ContentKind.LIVE -> "Canal"
}

fun epgFor(epg: Map<String, List<Programme>>, ch: Channel): Pair<Programme?, Programme?> =
    if (ch.tvgId.isBlank()) null to null else EpgRepository.nowNext(epg[ch.tvgId.lowercase()])

// ----- Tarjetas -----

/** Imagen de una película o serie; si no trae, sus iniciales sobre un color. */
@Composable
fun PosterArt(ch: Channel, modifier: Modifier = Modifier, fontSize: Float = 28f) {
    Box(modifier.background(Fc.tileFor(ch.name)), contentAlignment = Alignment.Center) {
        T(initials(ch.name), size = fontSize.sp, display = true, color = Color.White.copy(alpha = 0.9f))
        if (ch.logo.isNotBlank()) {
            AsyncImage(model = ch.logo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun VodCard(
    ch: Channel,
    recent: RecentItem?,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    width: Dp = 205.dp,
    focusRequester: FocusRequester? = null,
) {
    FocusBox(onClick = onClick, onFocus = onFocus, focusRequester = focusRequester, modifier = Modifier.size(width, 150.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                PosterArt(ch, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .padding(8.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xD10A1747))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { T(typeLabel(ch.kind), size = 12.sp, weight = FontWeight.SemiBold) }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xB30A1747))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(vodFraction(recent))
                            .height(4.dp)
                            .background(Fc.Menta)
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(ch.name, size = 15.sp, weight = FontWeight.SemiBold)
                T(remaining(recent), size = 13.sp, color = Fc.Muted)
            }
        }
    }
}

@Composable
fun RecentChannelCard(
    ch: Channel,
    now: Programme?,
    visited: Long,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    width: Dp = 205.dp,
    focusRequester: FocusRequester? = null,
) {
    FocusBox(onClick = onClick, onFocus = onFocus, focusRequester = focusRequester, modifier = Modifier.size(width, 98.dp)) { f ->
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ch.num > 0) T(ch.num.toString(), size = 24.sp, display = true, color = if (f) Color.White else Fc.Num)
                ChannelLogo(ch.name, ch.logo, 30.dp, fontSize = 9f)
                T(ch.name, size = 15.sp, weight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Fc.Menta))
                T(now?.title ?: "En vivo", size = 14.sp, color = Fc.Muted2)
            }
            T(visitedAgo(visited), size = 13.sp, color = Fc.Dim)
        }
    }
}

@Composable
fun ChannelCard(
    ch: Channel,
    now: Programme?,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    showGroup: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    FocusBox(onClick = onClick, onFocus = onFocus, focusRequester = focusRequester, modifier = modifier.height(124.dp)) { f ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ch.num > 0) T(ch.num.toString(), size = 24.sp, display = true, color = if (f) Color.White else Fc.Num)
                ChannelLogo(ch.name, ch.logo, 30.dp, fontSize = 9f)
                T(ch.name, size = 15.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (favorite) T("★", size = 14.sp, color = Fc.Menta)
            }
            val sub = now?.title ?: if (showGroup) ch.group else if (ch.kind != com.fitotv.data.ContentKind.LIVE) typeLabel(ch.kind) else ""
            T(sub, size = 14.sp, color = Fc.Muted2, maxLines = 2, lineHeight = 19.sp)
            Box(Modifier.weight(1f))
            if (now != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Fc.Track)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progressOf(now))
                            .height(4.dp)
                            .background(Fc.Menta)
                    )
                }
            }
        }
    }
}

/** Fondo con degradado para leer texto sobre video o imagen. */
fun bottomShade() = Brush.verticalGradient(listOf(Color(0x000A1747), Color(0xE00A1747), Color(0xF70A1747)))
