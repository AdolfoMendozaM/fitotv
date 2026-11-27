package com.fitotv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitotv.R
import com.fitotv.data.AvatarId
import com.fitotv.sound.Sfx
import com.fitotv.sound.Sounds
import com.fitotv.ui.Toast
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay

val LocalSounds = staticCompositionLocalOf<Sounds?> { null }

/** Íconos de línea en una cuadrícula de 24, los mismos del diseño. */
object Icons {
    const val HOME = "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z"
    const val GUIDE = "M3 6h18M3 12h18M3 18h18M8 3v18"
    const val FILM = "M4 4h16v16H4zM8 4v16M16 4v16M4 9h4M4 15h4M16 9h4M16 15h4"
    const val TV = "M3 8h18v12H3zM8 3l4 4 4-4"
    const val SEARCH = "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14zM20 20l-4-4"
    const val SETTINGS = "M4 7h9M19 7h1M4 17h3M13 17h7M16 4.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5zM10 14.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z"
    const val PLAY = "M8 5v14l11-7z"
    const val PAUSE = "M7 5h4v14H7zM13 5h4v14h-4z"
    const val STAR = "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"
    const val RESTART = "M4 12a8 8 0 1 0 2.5-5.8M4 4v5h5"
    const val EPISODES = "M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01"
    const val CHEVRON_DOWN = "M6 9l6 6 6-6"
    const val PLUS = "M12 5v14M5 12h14"
    const val MUTE = "M4 9v6h4l5 4V5L8 9zM17 9l4 6M21 9l-4 6"
    const val GRID = "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"
    const val HISTORY = "M12 7v5l3 2M3.5 12a8.5 8.5 0 1 0 2.5-6M3 4v4h4"
    const val CHECK = "M5 12.5l4.5 4.5L19 7.5"
    const val WARN = "M12 7v6M12 17h.01"
    const val EDIT = "M4 20h4L19 9l-4-4L4 16z"
    const val BACK = "M15 6l-6 6 6 6"
    const val UPDOWN = "M7 9l5-5 5 5M7 15l5 5 5-5"
    const val PHONE = "M7 3h10v18H7zM11 18h2"
    const val REMOTE = "M8 3h8v18H8zM12 7h.01M10 11h4M10 14h4"
    const val TRASH = "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"
    const val REFRESH = "M20 12a8 8 0 1 1-2.5-5.8M20 4v5h-5"
    const val INFO = "M12 8h.01M11 12h1v5h1M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"
    const val LIST = "M4 6h16M4 12h16M4 18h10"
    const val SOUND = "M4 9v6h4l5 4V5L8 9zM16 9a4 4 0 0 1 0 6M18.5 6.5a8 8 0 0 1 0 11"
    const val USER = "M12 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM4 21a8 8 0 0 1 16 0"
    const val NEWS = "M4 5h13v14H6a2 2 0 0 1-2-2zM17 9h3v8a2 2 0 0 1-2 2M8 9h5M8 13h5"
    const val BALL = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM3.5 12h17M12 3c-3 3-3 15 0 18M12 3c3 3 3 15 0 18"
    const val KIDS = "M12 3a6 6 0 0 1 6 6c0 4-3 7-6 7s-6-3-6-7a6 6 0 0 1 6-6zM12 16v5M10 21h4"
    const val MUSIC = "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0zM20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0z"
    const val GLOBE = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"
    const val PIN = "M12 21s-7-6.5-7-11a7 7 0 0 1 14 0c0 4.5-7 11-7 11zM12 7.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z"
    const val CLOCK = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7v5l3 2"
    const val BOOK = "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM4 5v16"
}

@Composable
fun Ico(path: String, size: Dp = 24.dp, color: Color = Fc.Text, stroke: Float = 2f, fill: Boolean = false, modifier: Modifier = Modifier) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            if (fill) drawPath(parsed, color, style = Fill)
            else drawPath(parsed, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Todo lo que se puede enfocar con el control: tarjetas, botones y renglones.
 * Al enfocarlo se dibuja el anillo blanco del diseño (con su espacio de separación).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    bg: Color = Fc.S2,
    focusedBg: Color = Fc.S3,
    ring: Boolean = true,
    ringColor: Color = Color.White,
    radius: Dp = 16.dp,
    sound: Sfx? = Sfx.OK,
    focusRequester: FocusRequester? = null,
    onFocus: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val sounds = LocalSounds.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                if (it.isFocused && !focused) {
                    sounds?.play(Sfx.TICK)
                    onFocus?.invoke()
                }
                if (it.isFocused != focused) onFocusChange?.invoke(it.isFocused)
                focused = it.isFocused
            }
            .drawBehind {
                if (focused && ring) {
                    val gap = 3.dp.toPx()
                    val w = 3.dp.toPx()
                    val o = gap + w / 2f
                    val r = radius.toPx() + o
                    drawRoundRect(
                        color = ringColor,
                        topLeft = Offset(-o, -o),
                        size = Size(size.width + o * 2, size.height + o * 2),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = w),
                    )
                }
            }
            .clip(shape)
            .background(if (focused) focusedBg else bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    sound?.let { s -> sounds?.play(s) }
                    onClick()
                },
                onLongClick = onLongClick,
            ),
        contentAlignment = contentAlignment,
    ) {
        content(focused)
    }
}

@Composable
fun ProgressBar(fraction: Float, width: Dp, height: Dp = 6.dp, color: Color = Fc.Menta, track: Color = Fc.Track) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

fun avatarRes(key: String?): Int = when (AvatarId.of(key)) {
    AvatarId.AUDIFONOS -> R.drawable.avatar_audifonos
    AvatarId.PELO_LARGO -> R.drawable.avatar_pelo_largo
    AvatarId.GORRA -> R.drawable.avatar_gorra
    AvatarId.CHONGO -> R.drawable.avatar_chongo
    AvatarId.COLETAS -> R.drawable.avatar_coletas
}

@Composable
fun AvatarImage(key: String?, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(avatarRes(key)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun LogoIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.logo_icono), contentDescription = "FitoTV", modifier = modifier.size(size))
}

@Composable
fun LogoFull(height: Dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(height * 0.22f)) {
        LogoIcon(height)
        T("FitoTV", size = (height.value * 0.62f).sp, display = true, letterSpacing = (-0.5).sp)
    }
}

private val stop = setOf("de", "del", "la", "el", "en", "y", "los", "las", "tv", "hd", "fhd", "sd", "4k", "canal")

fun initials(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
    words.firstOrNull { it.all(Char::isDigit) && it.length <= 3 }?.let { return it }
    val main = words.filter { it.lowercase() !in stop }.ifEmpty { words }
    return main.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "TV" }
}

/** Logo del canal; si no trae (o no carga), sus iniciales sobre un color. */
@Composable
fun ChannelLogo(name: String, logo: String, size: Dp, corner: Dp = 8.dp, fontSize: Float = 10f) =
    ChannelLogo(name, logo, size, size, corner, fontSize)

/**
 * Placa del logo, puede ser más ancha que alta: casi todos los logos de canal son horizontales.
 * Mientras el logo no carga (o si falla) se ven las iniciales sobre el color del canal.
 */
@Composable
fun ChannelLogo(name: String, logo: String, width: Dp, height: Dp, corner: Dp = 8.dp, fontSize: Float = 10f) {
    var loaded by remember(logo) { mutableStateOf(false) }
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(corner))
            .background(if (loaded) LogoPlate else Fc.tileFor(name)),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) T(initials(name), size = fontSize.sp, weight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        if (logo.isNotBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = width * 0.08f, vertical = height * 0.1f),
            )
        }
    }
}

/** Fondo neutro detrás de los logos: deja ver logos claros y de color sobre el azul. */
private val LogoPlate = Color(0x24FFFFFF)

@Composable
fun Pill(text: String, bg: Color = Fc.MentaSoft, fg: Color = Fc.Menta, height: Dp = 28.dp, fontSize: Float = 14f, dot: Color? = null) {
    Row(
        Modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        T(text, size = fontSize.sp, color = fg, weight = FontWeight.Bold)
    }
}

/** Botón principal azul (Continuar, Ver ahora, Siguiente…). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    height: Dp = 52.dp,
    focusRequester: FocusRequester? = null,
    bg: Color = Fc.Azul,
) {
    FocusBox(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(height / 2),
        radius = height / 2,
        bg = bg,
        focusedBg = bg,
        focusRequester = focusRequester,
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .padding(start = if (icon != null) 20.dp else 28.dp, end = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Ico(icon, 20.dp, Color.White, fill = icon == Icons.PLAY || icon == Icons.PAUSE)
            T(text, size = 18.sp, color = Color.White, weight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null, height: Dp = 52.dp, focusRequester: FocusRequester? = null) {
    FocusBox(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(height / 2),
        radius = height / 2,
        bg = Fc.S2,
        focusedBg = Fc.S3,
        focusRequester = focusRequester,
        sound = Sfx.BACK,
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .padding(start = if (icon != null) 20.dp else 26.dp, end = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Ico(icon, 20.dp, Fc.Text)
            T(text, size = 18.sp, weight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RoundIconButton(icon: String, onClick: () -> Unit, size: Dp = 52.dp, fill: Boolean = false, tint: Color = Fc.Text, focusRequester: FocusRequester? = null) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 2),
        radius = size / 2,
        bg = Fc.S2,
        focusedBg = Fc.S3,
        focusRequester = focusRequester,
        contentAlignment = Alignment.Center,
    ) {
        Ico(icon, size * 0.42f, tint, fill = fill, stroke = 2.2f)
    }
}

@Composable
fun Keycap(label: String? = null, icon: String? = null) {
    Box(
        Modifier
            .height(26.dp)
            .border(1.5.dp, Color(0xFF5C6FB0), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) T(label, size = 12.sp, weight = FontWeight.Bold)
        if (icon != null) Ico(icon, 14.dp, Fc.Text, stroke = 2.6f)
    }
}

@Composable
fun SectionTitle(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        T(title, size = 24.sp, display = true)
        if (sub != null) T(sub, size = 15.sp, color = Fc.Muted, modifier = Modifier.padding(bottom = 2.dp))
    }
}

/** Aviso discreto abajo a la derecha que se va solo. */
@Composable
fun ToastHost(toast: Toast?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            visible = true
            delay(5000)
            visible = false
            delay(400)
            onDone()
        }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = visible && toast != null,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut(),
            modifier = Modifier.padding(end = 56.dp, bottom = 28.dp),
        ) {
            val t = toast ?: return@AnimatedVisibility
            Row(
                Modifier
                    .width(430.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xF512235F))
                    .border(1.dp, Fc.Line2, RoundedCornerShape(18.dp))
                    .padding(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (t.warn) Fc.Ambar else Fc.Menta),
                    contentAlignment = Alignment.Center,
                ) {
                    Ico(if (t.warn) Icons.WARN else Icons.CHECK, 22.dp, Fc.Noche, stroke = 2.6f)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    T(t.title, size = 17.sp, weight = FontWeight.Bold, maxLines = 2)
                    T(t.text, size = 15.sp, color = Fc.Muted2, maxLines = 3, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
fun Gap(h: Dp = 0.dp, w: Dp = 0.dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(h).width(w))
}
