package com.fitotv.player

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Handler
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fitotv.data.Channel
import com.fitotv.data.Http
import com.fitotv.ui.components.ChannelLogo
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay

/**
 * Un reproductor listo para listas IPTV: HLS, MPEG-TS y archivos de video.
 *
 * Calidad: arranca suponiendo buena conexión (10 Mbps) para empezar en HD en vez de la calidad más baja,
 * y recuerda la velocidad medida entre canales. El búfer no cambia, así que no agrega retraso.
 * La vista previa se limita a 720p porque se ve chica y así no le quita internet al canal grande.
 */
@OptIn(UnstableApi::class)
class FitoPlayer(context: Context, muted: Boolean, preview: Boolean = false) {

    companion object {
        @Volatile
        private var meter: DefaultBandwidthMeter? = null

        /** Uno para toda la app: lo que aprende de tu internet sirve para el siguiente canal. */
        fun bandwidthMeter(context: Context): DefaultBandwidthMeter = meter ?: synchronized(this) {
            meter ?: DefaultBandwidthMeter.Builder(context.applicationContext)
                .setInitialBitrateEstimate(10_000_000L)
                .build()
                .also { meter = it }
        }
    }

    private val dataSource = OkHttpDataSource.Factory(Http.client).setUserAgent(Http.USER_AGENT)

    val exo: ExoPlayer
    private val selector: DefaultTrackSelector

    // Si el decodificador de sonido de la tele falla con un canal, se usa el de FitoTV (FFmpeg) y,
    // si tampoco se puede, el canal sigue con imagen y sin sonido en vez de dejar la pantalla en negro.
    @Volatile
    private var softAudio = false
    private var noAudio = false
    private var playing = ""

    init {
        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setConstantBitrateSeekingEnabled(true)
        val sources = DefaultMediaSourceFactory(dataSource, extractors)
        // La vista previa arranca con medio segundo de video: es chica y lo que importa es que se vea ya.
        val load = DefaultLoadControl.Builder()
            .apply {
                if (preview) setBufferDurationsMs(2_000, 15_000, 500, 1_000)
                else setBufferDurationsMs(12_000, 50_000, 1_000, 2_500)
            }
            .build()
        // Sube a mejor calidad tras 5 s estables (por defecto son 10) y usa 75 % del ancho medido.
        val adaptive = AdaptiveTrackSelection.Factory(
            5_000,
            AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
            AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
            0.75f,
        )
        selector = DefaultTrackSelector(context, adaptive)
        if (preview) selector.setParameters(selector.buildUponParameters().setMaxVideoSize(1280, 720))
        exo = ExoPlayer.Builder(context, FitoRenderers(context) { softAudio })
            .setMediaSourceFactory(sources)
            .setLoadControl(load)
            .setTrackSelector(selector)
            .setBandwidthMeter(bandwidthMeter(context))
            .build()
        if (muted) exo.volume = 0f
        exo.addAnalyticsListener(AudioLog { playing })
    }

    /**
     * Si el error vino del sonido, lo arregla y vuelve a intentar sin avisar a la pantalla.
     * Devuelve true cuando ya se está reintentando.
     */
    fun recoverAudio(e: PlaybackException): Boolean {
        val ex = e as? ExoPlaybackException ?: return false
        if (ex.type != ExoPlaybackException.TYPE_RENDERER) return false
        val mime = ex.rendererFormat?.sampleMimeType
        val audio = (mime != null && MimeTypes.isAudio(mime)) ||
            e.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
            e.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
        if (!audio) return false
        when {
            !softAudio && mime != null && ffmpegHandles(mime) -> {
                softAudio = true
                Log.w(AudioLog.TAG, "canal=$playing: falló el sonido de la tele con $mime; ahora con FFmpeg")
            }
            !noAudio -> {
                noAudio = true
                selector.setParameters(selector.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true))
                Log.w(AudioLog.TAG, "canal=$playing: no se pudo reproducir el sonido ($mime); sigue sin sonido")
            }
            else -> return false
        }
        exo.prepare()
        return true
    }

    fun play(channel: Channel, startMs: Long = 0) {
        playing = channel.name
        if (noAudio) {
            noAudio = false
            selector.setParameters(selector.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false))
        }
        val headers = HashMap<String, String>()
        if (channel.referrer.isNotBlank()) headers["Referer"] = channel.referrer
        dataSource.setDefaultRequestProperties(headers)
        dataSource.setUserAgent(channel.userAgent.ifBlank { Http.USER_AGENT })
        exo.stop()
        exo.setMediaItem(item(channel), startMs.coerceAtLeast(0))
        exo.prepare()
        exo.playWhenReady = true
    }

    fun stop() {
        exo.stop()
        exo.clearMediaItems()
    }

    fun release() = exo.release()

    private fun item(ch: Channel): MediaItem {
        val b = MediaItem.Builder().setUri(ch.url)
        val path = ch.url.substringBefore('?').lowercase()
        when {
            path.endsWith(".m3u8") || path.endsWith(".m3u") || path.contains("/hls/") -> b.setMimeType(MimeTypes.APPLICATION_M3U8)
            path.endsWith(".ts") -> b.setMimeType(MimeTypes.VIDEO_MP2T)
            // Canales de Xtream sin extensión (".../usuario/clave/12345"): son MPEG-TS. Decirlo ahorra
            // que el reproductor tenga que adivinar el formato antes de empezar.
            ch.kind == com.fitotv.data.ContentKind.LIVE && path.substringAfterLast('/').let { it.isNotEmpty() && it.all { c -> c.isDigit() } } ->
                b.setMimeType(MimeTypes.VIDEO_MP2T)
        }
        return b.build()
    }
}

@OptIn(UnstableApi::class)
private fun ffmpegHandles(mime: String): Boolean = runCatching { FfmpegLibrary.supportsFormat(mime) }.getOrDefault(false)

/**
 * Decodificadores de sonido: primero los de la tele (así el Dolby sigue llegando a la barra de sonido)
 * y, para lo que la tele no sabe tocar, el de FitoTV con FFmpeg. Muchos canales de México traen el
 * sonido en Dolby (AC-3/E-AC-3), MP2 o DTS; sin esto se ven pero no suenan en varias teles.
 * Con [soft] en true, todo lo que FFmpeg sabe tocar lo toca FFmpeg (para teles cuyo decodificador falla).
 */
@OptIn(UnstableApi::class)
private class FitoRenderers(context: Context, private val soft: () -> Boolean) : DefaultRenderersFactory(context) {
    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        setEnableDecoderFallback(true)
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        val adapters = codecAdapterFactory
        val forceSoft = soft
        out.add(object : MediaCodecAudioRenderer(
            context, adapters, mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, audioSink,
        ) {
            override fun supportsFormat(mediaCodecSelector: MediaCodecSelector, format: Format): Int {
                val mime = format.sampleMimeType
                if (forceSoft() && mime != null && ffmpegHandles(mime)) {
                    return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE)
                }
                return super.supportsFormat(mediaCodecSelector, format)
            }
        })
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, audioSink))
    }
}

/** Deja en el registro de la tele qué sonido trae cada canal y quién lo toca (para revisar "no suena"). */
@OptIn(UnstableApi::class)
private class AudioLog(private val channel: () -> String) : AnalyticsListener {
    companion object {
        const val TAG = "FitoAudio"
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        Log.i(TAG, "canal=${channel()}: sonido ${format.sampleMimeType} ${format.channelCount} canales ${format.sampleRate} Hz")
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        Log.i(TAG, "canal=${channel()}: decodificador $decoderName")
    }

    override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
        Log.i(TAG, "canal=${channel()}: ya suena")
    }

    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
        for (g in tracks.groups) {
            if (g.type == C.TRACK_TYPE_AUDIO && !g.isSupported) {
                Log.w(TAG, "canal=${channel()}: sonido sin decodificador ${g.getTrackFormat(0).sampleMimeType}")
            }
        }
    }
}

/** Superficie de video sin controles (los controles los dibuja FitoTV). */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(player: FitoPlayer, modifier: Modifier = Modifier, zoom: Boolean = false) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                useController = false
                setShutterBackgroundColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
                setKeepContentOnPlayerReset(false)
                resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                isFocusable = false
                isFocusableInTouchMode = false
                this.player = player.exo
            }
        },
    )
}

/**
 * Vista previa (con sonido) del canal enfocado. Espera un momento antes de cargar
 * para no abrir cada canal por el que pasas rápido. Mientras carga (o si el canal
 * no responde) se ve el logo del canal en lugar de un cuadro negro.
 */
@Composable
fun LivePreview(channel: Channel?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember { FitoPlayer(context, muted = false, preview = true) }
    var state by remember { mutableIntStateOf(0) } // 0 cargando, 1 se ve, 2 no responde
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                state = 1
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!player.recoverAudio(error)) state = 2
            }
        }
        player.exo.addListener(listener)
        onDispose {
            player.exo.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(channel?.id) {
        state = 0
        player.stop()
        if (channel != null) {
            delay(450)
            player.play(channel)
        }
    }
    Box(modifier) {
        VideoSurface(player, Modifier.fillMaxSize(), zoom = true)
        if (channel != null && state != 1) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Fc.S3),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ChannelLogo(channel.name, channel.logo, width = 128.dp, height = 76.dp, corner = 18.dp, fontSize = 22f)
                    Spacer(Modifier.height(12.dp))
                    T(if (state == 2) "Sin vista previa por ahora" else "Cargando vista previa…", size = 14.sp, color = Fc.Muted)
                }
            }
        }
    }
}
