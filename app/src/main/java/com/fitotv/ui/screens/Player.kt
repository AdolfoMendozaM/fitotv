package com.fitotv.ui.screens

import android.view.KeyEvent as AndroidKey
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import com.fitotv.data.Channel
import com.fitotv.data.ContentKind
import com.fitotv.player.FitoPlayer
import com.fitotv.player.VideoSurface
import com.fitotv.sound.Sfx
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.AvatarImage
import com.fitotv.ui.components.ChannelLogo
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.Keycap
import com.fitotv.ui.components.Pill
import com.fitotv.ui.components.ProgressBar
import com.fitotv.ui.components.Spinner
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(vm: AppViewModel, s: Screen.Player) {
    val context = LocalContext.current
    val d by vm.data.collectAsState()
    val zap = remember(s) { s.zap.mapNotNull { vm.channel(it) } }
    var current by remember(s) { mutableStateOf(vm.channel(s.channelId)) }
    val player = remember { FitoPlayer(context, muted = false) }
    var showInfo by remember { mutableStateOf(true) }
    var infoKey by remember { mutableIntStateOf(0) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retried by remember { mutableStateOf(false) }
    var retryAt by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var digits by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    val isVod = current?.kind != null && current?.kind != ContentKind.LIVE
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var panel by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val audioOpts = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_AUDIO) }
    val textOpts = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_TEXT) }
    val hasOptions = audioOpts.size > 1 || textOpts.isNotEmpty()
    val maxScreens = vm.activeList(d)?.maxScreens ?: 0

    // El idioma y los subtítulos que elegiste la última vez se usan en todas las películas y series.
    LaunchedEffect(Unit) {
        val st = vm.data.value.settings
        player.exo.trackSelectionParameters = player.exo.trackSelectionParameters.buildUpon().apply {
            if (st.audioLang.isNotBlank()) setPreferredAudioLanguage(st.audioLang)
            when {
                st.subLang == "off" -> setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                st.subLang.isNotBlank() -> setPreferredTextLanguage(st.subLang)
            }
        }.build()
    }

    fun chooseAudio(o: TrackOption) {
        player.exo.trackSelectionParameters = player.exo.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(o.group.mediaTrackGroup, o.index))
            .setPreferredAudioLanguage(o.format.language)
            .build()
        vm.setTrackPrefs(audioLang = o.format.language ?: "")
    }

    fun chooseText(o: TrackOption?) {
        val b = player.exo.trackSelectionParameters.buildUpon()
        if (o == null) {
            b.clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            vm.setTrackPrefs(subLang = "off")
        } else {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(o.group.mediaTrackGroup, o.index))
                .setPreferredTextLanguage(o.format.language)
            vm.setTrackPrefs(subLang = o.format.language ?: "")
        }
        player.exo.trackSelectionParameters = b.build()
    }

    fun saveProgress() {
        val ch = current ?: return
        if (ch.kind == ContentKind.LIVE) return
        val pos = player.exo.currentPosition
        val dur = player.exo.duration
        if (dur > 0 && pos > 5_000) vm.recordRecent(ch, pos, dur)
    }

    fun exit() {
        saveProgress()
        vm.back()
    }

    fun bumpInfo() {
        showInfo = true
        infoKey += 1
    }

    fun zapBy(delta: Int) {
        val ch = current ?: return
        val pool = zap.ifEmpty { vm.channelsOf(vm.selectedGroup) }
        if (pool.size < 2) return
        val idx = pool.indexOfFirst { it.id == ch.id }.coerceAtLeast(0)
        current = pool[(idx + delta + pool.size) % pool.size]
        vm.sounds.play(Sfx.TICK)
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING || retryAt > 0
                if (state == Player.STATE_READY) {
                    error = null
                    attempts = 0
                }
                if (state == Player.STATE_ENDED) {
                    val ch = current
                    if (ch != null && ch.kind != ContentKind.LIVE) {
                        val dur = player.exo.duration
                        vm.recordRecent(ch, dur, dur)
                        // En series sigue el próximo episodio de la temporada.
                        val idx = zap.indexOfFirst { it.id == ch.id }
                        val next = if (ch.kind == ContentKind.SERIES && idx >= 0) zap.getOrNull(idx + 1) else null
                        if (next != null) current = next else vm.back()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onTracksChanged(t: Tracks) {
                tracks = t
            }

            override fun onPlayerError(e: PlaybackException) {
                if (player.recoverAudio(e)) return
                // Hasta 3 reintentos callados (el proveedor a veces tarda en soltar la conexión anterior).
                if (attempts < 3 && retryable(e)) {
                    attempts += 1
                    buffering = true
                    retryAt = System.currentTimeMillis() + 1_500L * attempts
                    return
                }
                buffering = false
                error = describe(e, maxScreens)
            }
        }
        player.exo.addListener(listener)
        onDispose {
            saveProgress()
            player.exo.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(current?.id) {
        val ch = current ?: return@LaunchedEffect
        error = null
        retried = false
        attempts = 0
        retryAt = 0L
        tracks = Tracks.EMPTY
        buffering = true
        val start = if (ch.id == s.channelId) s.resumeMs else 0L
        player.play(ch, if (start > 30_000) start - 3_000 else 0L)
        if (ch.kind == ContentKind.LIVE) vm.recordRecent(ch)
        bumpInfo()
    }

    // La barra de información se esconde sola (en canales) para no tapar.
    LaunchedEffect(showInfo, infoKey, playing) {
        if (showInfo && (!isVod || playing)) {
            delay(5_000)
            showInfo = false
        }
    }

    // Avance de películas y series.
    LaunchedEffect(current?.id) {
        var ticks = 0
        while (true) {
            position = player.exo.currentPosition
            duration = player.exo.duration.coerceAtLeast(0L)
            delay(500)
            ticks += 1
            if (ticks % 30 == 0) saveProgress()
        }
    }

    // Reintento callado: mientras tanto se ve el círculo de carga, no un error.
    LaunchedEffect(retryAt) {
        if (retryAt <= 0) return@LaunchedEffect
        delay((retryAt - System.currentTimeMillis()).coerceAtLeast(0))
        retryAt = 0L
        val ch = current ?: return@LaunchedEffect
        val pos = if (ch.kind != ContentKind.LIVE) player.exo.currentPosition else 0L
        player.play(ch, pos)
    }

    // Un reintento automático si el canal falla.
    LaunchedEffect(error) {
        val ch = current
        if (error != null && !retried && ch != null) {
            delay(2_500)
            retried = true
            player.play(ch)
        }
    }

    // Saltar a un canal tecleando su número.
    LaunchedEffect(digits) {
        if (digits.isNotEmpty()) {
            delay(1_600)
            val n = digits.toIntOrNull()
            val target = vm.lib.live.firstOrNull { it.num == n }
            if (target != null) current = target else vm.sounds.play(Sfx.BACK)
            digits = ""
        }
    }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { fr.requestFocus() }
    }

    BackHandler {
        when {
            panel -> {
                panel = false
                runCatching { fr.requestFocus() }
            }
            digits.isNotEmpty() -> digits = ""
            showInfo && !isVod -> showInfo = false
            else -> exit()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(fr)
            .focusable()
            .onPreviewKeyEvent { e ->
                // Con el panel de idioma abierto, las flechas y OK son del panel.
                if (panel) return@onPreviewKeyEvent false
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val code = e.nativeKeyEvent.keyCode
                when {
                    hasOptions && (code == AndroidKey.KEYCODE_MENU || code == AndroidKey.KEYCODE_CAPTIONS ||
                        code == AndroidKey.KEYCODE_MEDIA_AUDIO_TRACK || (isVod && code == AndroidKey.KEYCODE_DPAD_DOWN) ||
                        (!isVod && code == AndroidKey.KEYCODE_DPAD_RIGHT)) -> {
                        panel = true
                        true
                    }
                    code in AndroidKey.KEYCODE_0..AndroidKey.KEYCODE_9 && !isVod -> {
                        if (digits.length < 4) digits += (code - AndroidKey.KEYCODE_0).toString()
                        true
                    }
                    code == AndroidKey.KEYCODE_DPAD_UP || code == AndroidKey.KEYCODE_CHANNEL_UP -> {
                        if (!isVod) zapBy(+1) else bumpInfo()
                        true
                    }
                    code == AndroidKey.KEYCODE_DPAD_DOWN || code == AndroidKey.KEYCODE_CHANNEL_DOWN -> {
                        if (!isVod) zapBy(-1) else bumpInfo()
                        true
                    }
                    code == AndroidKey.KEYCODE_DPAD_CENTER || code == AndroidKey.KEYCODE_ENTER ||
                        code == AndroidKey.KEYCODE_NUMPAD_ENTER || code == AndroidKey.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (isVod) {
                            if (player.exo.isPlaying) player.exo.pause() else player.exo.play()
                            bumpInfo()
                        } else if (error != null) {
                            current?.let { player.play(it) }
                            error = null
                        } else {
                            showInfo = !showInfo
                            if (showInfo) infoKey += 1
                        }
                        true
                    }
                    isVod && (code == AndroidKey.KEYCODE_DPAD_RIGHT || code == AndroidKey.KEYCODE_MEDIA_FAST_FORWARD) -> {
                        val step = if (code == AndroidKey.KEYCODE_DPAD_RIGHT) 10_000 else 30_000
                        player.exo.seekTo((player.exo.currentPosition + step).coerceAtMost((player.exo.duration - 1_000).coerceAtLeast(0)))
                        bumpInfo()
                        true
                    }
                    isVod && (code == AndroidKey.KEYCODE_DPAD_LEFT || code == AndroidKey.KEYCODE_MEDIA_REWIND) -> {
                        val step = if (code == AndroidKey.KEYCODE_DPAD_LEFT) 10_000 else 30_000
                        player.exo.seekTo((player.exo.currentPosition - step).coerceAtLeast(0))
                        bumpInfo()
                        true
                    }
                    !isVod && code == AndroidKey.KEYCODE_DPAD_LEFT -> {
                        exit()
                        vm.openTab(Screen.Guide)
                        true
                    }
                    else -> false
                }
            }
    ) {
        VideoSurface(player, Modifier.fillMaxSize())

        if (buffering && error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(56.dp) }
        }

        error?.let { msg ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Fc.Ambar), contentAlignment = Alignment.Center) {
                    Ico(Icons.WARN, 40.dp, Fc.Noche, stroke = 2.8f)
                }
                Gap(h = 20.dp)
                T(current?.name ?: "", size = 20.sp, color = Fc.Muted2)
                Gap(h = 6.dp)
                T(msg, size = 30.sp, display = true, align = TextAlign.Center, maxLines = 2)
                Gap(h = 14.dp)
                T(
                    if (isVod) "Presiona OK para intentar otra vez o Atrás para salir." else "Presiona OK para intentar otra vez o cambia de canal con ▲▼.",
                    size = 18.sp, color = Fc.Muted,
                )
            }
        }

        val ch = current
        AnimatedVisibility(visible = !panel && (showInfo || (isVod && !playing && !buffering)) && ch != null && error == null, enter = fadeIn(), exit = fadeOut()) {
            if (ch != null) {
                if (isVod) VodBanner(ch, position, duration, playing, hasOptions)
                else LiveBanner(vm, ch, vm.profile(d)?.avatar, vm.profile(d)?.name ?: "", vm.activeList(d)?.name ?: "", hasOptions)
            }
        }

        if (panel) {
            TracksPanel(
                audio = audioOpts,
                text = textOpts,
                onAudio = { chooseAudio(it) },
                onText = { chooseText(it) },
            )
        }

        if (digits.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xE612235F))
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                T(digits, size = 56.sp, display = true, color = Fc.Num)
            }
        }
    }
}

/** Errores que vale la pena reintentar solos: de red o del servidor (no de formato). */
private fun retryable(e: PlaybackException): Boolean = when (e.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> true
    else -> false
}

/** El código HTTP con el que contestó el proveedor, si fue eso. */
private fun httpCode(e: PlaybackException): Int? {
    var c: Throwable? = e.cause
    repeat(4) {
        if (c is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) return (c as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException).responseCode
        c = c?.cause
    }
    return null
}

private fun describe(e: PlaybackException, maxScreens: Int = 0): String = when (e.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> when (httpCode(e)) {
        401, 403, 406, 429, 458, 509, 512, 551, 552 ->
            if (maxScreens > 0) "Tu proveedor no dejó abrir el canal. Tu cuenta permite $maxScreens ${if (maxScreens == 1) "pantalla" else "pantallas"} a la vez: cierra FitoTV en otra tele."
            else "Tu proveedor no dejó abrir el canal. Si la lista está abierta en otra tele, ciérrala."
        404, 410 -> "Este canal ya no existe en tu proveedor."
        in 500..599 -> "El canal está fallando del lado de tu proveedor."
        else -> "El canal no respondió. Puede estar caído o la cuenta venció."
    }
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "No pudimos conectarnos con el canal."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "Este formato de video no se puede reproducir en esta tele."
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "Se perdió la señal un momento."
    else -> "Este canal no está disponible ahora."
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun Shade(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Brush.verticalGradient(listOf(Color(0xBF0A1747), Color(0x000A1747))))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(330.dp)
                .background(Brush.verticalGradient(listOf(Color(0x000A1747), Color(0xE00A1747), Color(0xF70A1747))))
        )
        content()
    }
}

@Composable
private fun LiveBanner(vm: AppViewModel, ch: Channel, avatar: String?, profileName: String, listName: String, hasOptions: Boolean) {
    val (now, next) = vm.epgFor(ch)
    Shade {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .padding(start = 56.dp, top = 40.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE612235F))
                    .padding(start = 6.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(avatar, 36.dp)
                T(profileName, size = 17.sp, weight = FontWeight.SemiBold)
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF5C6FB0)))
                T(listName, size = 17.sp, color = Fc.Muted2)
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 56.dp, end = 56.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Pill("En vivo", bg = Color(0xE612235F), fg = Fc.Text, height = 32.dp, fontSize = 15f, dot = Fc.Menta)
                Row(verticalAlignment = Alignment.Bottom) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        if (ch.num > 0) T(ch.num.toString(), size = 60.sp, display = true, color = Fc.Num)
                        ChannelLogo(ch.name, ch.logo, width = 104.dp, height = 60.dp, corner = 14.dp, fontSize = 18f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            T(ch.name, size = 18.sp, color = Fc.Muted2)
                            T(now?.title ?: "En vivo", size = 36.sp, display = true, letterSpacing = (-0.5).sp)
                            if (now != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    T("${hhmm(now.start)} a ${hhmm(now.stop)}", size = 16.sp, color = Fc.Muted2)
                                    ProgressBar(progressOf(now), 200.dp)
                                    T(minutesLeft(now), size = 16.sp, color = Fc.Muted2)
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (next != null) T("Después, a las ${hhmm(next.start)}: ${next.title}", size = 16.sp, color = Fc.Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Keycap(icon = Icons.UPDOWN)
                                T("Cambiar de canal", size = 15.sp, color = Fc.Muted2)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Keycap(icon = Icons.BACK)
                                T("Guía", size = 15.sp, color = Fc.Muted2)
                            }
                            if (hasOptions) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Keycap(label = "▶")
                                    T("Idioma y subtítulos", size = 15.sp, color = Fc.Muted2)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Keycap(label = "OK")
                                T("Ocultar", size = 15.sp, color = Fc.Muted2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VodBanner(ch: Channel, position: Long, duration: Long, playing: Boolean, hasOptions: Boolean) {
    Shade {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 56.dp, end = 56.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill(typeLabel(ch.kind))
                T(ch.group, size = 16.sp, color = Fc.Muted2)
            }
            Gap(h = 10.dp)
            T(ch.name, size = 40.sp, display = true, letterSpacing = (-0.5).sp)
            Gap(h = 18.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Ico(if (playing) Icons.PAUSE else Icons.PLAY, 28.dp, Color.White, fill = true)
                T(clock(position), size = 16.sp, color = Fc.Muted2)
                Box(Modifier.weight(1f)) {
                    ProgressBar(if (duration > 0) position.toFloat() / duration else 0f, 2000.dp, height = 6.dp)
                }
                T(if (duration > 0) clock(duration) else "--:--", size = 16.sp, color = Fc.Muted2)
            }
            Gap(h = 16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Keycap(label = "OK")
                    T(if (playing) "Pausa" else "Seguir", size = 15.sp, color = Fc.Muted2)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Keycap(label = "◀ ▶")
                    T("10 segundos", size = 15.sp, color = Fc.Muted2)
                }
                if (hasOptions) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Keycap(label = "▼")
                        T("Idioma y subtítulos", size = 15.sp, color = Fc.Muted2)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Keycap(label = "Atrás")
                    T("Salir y guardar dónde vas", size = 15.sp, color = Fc.Muted2)
                }
            }
        }
    }
}

// ----- Idioma y subtítulos -----

/** Una pista de audio o de subtítulos que se puede elegir. */
class TrackOption(val group: Tracks.Group, val index: Int, val format: Format, val label: String, val selected: Boolean)

private val esMx = java.util.Locale("es", "MX")

/** Las pistas que el reproductor sabe tocar, con nombres en español ("Español", "Inglés · 5.1"). */
fun trackOptions(tracks: Tracks, type: Int): List<TrackOption> {
    val out = ArrayList<TrackOption>()
    var n = 0
    for (g in tracks.groups) {
        if (g.type != type) continue
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            n += 1
            val f = g.getTrackFormat(i)
            out += TrackOption(g, i, f, trackLabel(f, n, type), g.isTrackSelected(i))
        }
    }
    // Mismo nombre dos veces (dos pistas en español): se numeran para distinguirlas.
    val dup = out.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys
    if (dup.isEmpty()) return out
    val seen = HashMap<String, Int>()
    return out.map { o ->
        if (o.label !in dup) o else {
            val k = (seen[o.label] ?: 0) + 1
            seen[o.label] = k
            TrackOption(o.group, o.index, o.format, "${o.label} $k", o.selected)
        }
    }
}

private fun trackLabel(f: Format, n: Int, type: Int): String {
    val code = f.language?.takeIf { it.isNotBlank() && it != "und" && it != "zxx" && it != "mul" }
    val langName = code?.let {
        val name = java.util.Locale.forLanguageTag(it).getDisplayLanguage(esMx)
        if (name.isBlank() || name.equals(it, ignoreCase = true)) null else name.replaceFirstChar { c -> c.uppercaseChar() }
    }
    val label = f.label?.trim()?.takeIf { it.isNotEmpty() }
    var base = when {
        label != null && langName != null && !label.contains(langName, ignoreCase = true) -> "$langName · $label"
        label != null -> label
        langName != null -> langName
        type == C.TRACK_TYPE_AUDIO -> "Audio $n"
        else -> "Subtítulos $n"
    }
    if (type == C.TRACK_TYPE_AUDIO && f.channelCount >= 6) base += " · 5.1"
    if (type == C.TRACK_TYPE_TEXT && (f.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) base += " (solo partes en otro idioma)"
    return base
}

@Composable
private fun TracksPanel(
    audio: List<TrackOption>,
    text: List<TrackOption>,
    onAudio: (TrackOption) -> Unit,
    onText: (TrackOption?) -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { first.requestFocus() }
    }
    val subsOff = text.none { it.selected }
    val firstAudio = audio.indexOfFirst { it.selected }.coerceAtLeast(0)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0x000A1747), Color(0xCC0A1747), Color(0xF20A1747))))
        )
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(560.dp)
                .fillMaxHeight()
                .padding(start = 40.dp, end = 56.dp, top = 56.dp, bottom = 40.dp),
        ) {
            T("Idioma y subtítulos", size = 30.sp, display = true)
            Gap(h = 6.dp)
            T("Se recuerda para tus siguientes películas y series.", size = 15.sp, color = Fc.Muted)
            Gap(h = 22.dp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
                if (audio.size > 1) {
                    item(key = "a") { T("Idioma", size = 17.sp, weight = FontWeight.SemiBold, color = Fc.Muted2) }
                    items(audio.size, key = { "a$it" }) { i ->
                        val o = audio[i]
                        TrackRow(o.label, o.selected, if (i == firstAudio) first else null) { onAudio(o) }
                    }
                    item(key = "gap") { Gap(h = 12.dp) }
                }
                if (text.isNotEmpty()) {
                    item(key = "t") { T("Subtítulos", size = 17.sp, weight = FontWeight.SemiBold, color = Fc.Muted2) }
                    item(key = "off") {
                        TrackRow("Sin subtítulos", subsOff, if (audio.size <= 1 && subsOff) first else null) { onText(null) }
                    }
                    items(text.size, key = { "t$it" }) { i ->
                        val o = text[i]
                        TrackRow(o.label, o.selected, if (audio.size <= 1 && o.selected) first else null) { onText(o) }
                    }
                }
            }
            Box(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Keycap(label = "Atrás")
                T("Cerrar", size = 15.sp, color = Fc.Muted2)
            }
        }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, fr: FocusRequester?, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        focusRequester = fr,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        bg = if (selected) Fc.S3 else Fc.S1,
        focusedBg = Fc.Azul,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (selected) Ico(Icons.CHECK, 22.dp, Fc.Menta, stroke = 2.6f)
            }
            T(label, size = 18.sp, weight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
