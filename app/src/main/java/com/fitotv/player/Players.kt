package com.fitotv.player

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fitotv.data.Channel
import com.fitotv.data.Http
import kotlinx.coroutines.delay

/** Un reproductor listo para listas IPTV: HLS, MPEG-TS y archivos de video. */
@OptIn(UnstableApi::class)
class FitoPlayer(context: Context, muted: Boolean) {

    private val dataSource = OkHttpDataSource.Factory(Http.client).setUserAgent(Http.USER_AGENT)

    val exo: ExoPlayer

    init {
        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setConstantBitrateSeekingEnabled(true)
        val sources = DefaultMediaSourceFactory(dataSource, extractors)
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(12_000, 50_000, 1_200, 2_500)
            .build()
        exo = ExoPlayer.Builder(context)
            .setMediaSourceFactory(sources)
            .setLoadControl(load)
            .build()
        if (muted) exo.volume = 0f
    }

    fun play(channel: Channel, startMs: Long = 0) {
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
        }
        return b.build()
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
 * Vista previa sin sonido del canal enfocado. Espera un momento antes de cargar
 * para no abrir cada canal por el que pasas rápido.
 */
@Composable
fun LivePreview(channel: Channel?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember { FitoPlayer(context, muted = true) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(channel?.id) {
        player.stop()
        if (channel != null) {
            delay(700)
            player.play(channel)
        }
    }
    VideoSurface(player, modifier, zoom = true)
}
