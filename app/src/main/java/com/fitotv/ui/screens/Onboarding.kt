package com.fitotv.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.data.AvatarId
import com.fitotv.sound.Sfx
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.AvatarImage
import com.fitotv.ui.components.FocusBox
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.LogoFull
import com.fitotv.ui.components.LogoIcon
import com.fitotv.ui.components.Pill
import com.fitotv.ui.components.PrimaryButton
import com.fitotv.ui.components.ProgressBar
import com.fitotv.ui.components.SecondaryButton
import com.fitotv.ui.components.Spinner
import com.fitotv.ui.components.Switch
import com.fitotv.ui.components.TvTextField
import com.fitotv.ui.rememberAutoFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Pantalla de carga
// ---------------------------------------------------------------------------

private fun Modifier.splashBackground() = drawBehind {
    drawRect(
        Brush.radialGradient(
            0f to Fc.Glow,
            0.62f to Fc.Noche,
            center = Offset(size.width * 0.5f, size.height * 0.4f),
            radius = size.width * 0.603f,
        )
    )
}

@Composable
private fun SplashArt(fill: Float, textAlpha: Float, label: String) {
    val waves = rememberInfiniteTransition(label = "ondas")
    val w1 by waves.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "onda1")
    Box(
        Modifier
            .fillMaxSize()
            .splashBackground()
    ) {
        Canvas(
            Modifier
                .offset(520.dp, 150.dp)
                .size(240.dp, 64.dp)
        ) {
            val k = size.width / 240f
            fun wave(p: Float, d: String) {
                val a = when {
                    p < 0.35f -> p / 0.35f
                    else -> 1f - (p - 0.35f) / 0.65f
                }.coerceIn(0f, 1f)
                val s = 0.7f + 0.42f * p
                val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath()
                withTransform({
                    scale(s * k, s * k, pivot = Offset(120f * k, 62f * k))
                }) {
                    drawPath(path, Fc.Menta.copy(alpha = a), style = Stroke(width = 5f, cap = StrokeCap.Round))
                }
            }
            wave(w1, "M78 60 Q120 22 162 60")
            wave((w1 + 0.667f) % 1f, "M52 62 Q120 -4 188 62")
        }
        LogoIcon(168.dp, Modifier.offset(556.dp, 196.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = 386.dp)
                .alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            T("FitoTV", size = 64.sp, display = true, letterSpacing = (-1.5).sp)
            Gap(h = 44.dp)
            ProgressBar(fill, 240.dp)
            Gap(h = 16.dp)
            T(label, size = 18.sp, color = Fc.Muted2)
        }
        T(
            "Tu tele, tus listas.",
            size = 15.sp,
            color = Fc.Dim,
            align = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(textAlpha),
        )
    }
}

@Composable
fun SplashScreen(vm: AppViewModel) {
    val d = vm.data.collectAsState().value
    val returning = d.profiles.isNotEmpty()
    val fill = remember { Animatable(0f) }
    var textOut by remember { mutableStateOf(false) }
    val textAlpha by animateFloatAsState(if (textOut) 0f else 1f, tween(350), label = "texto")

    LaunchedEffect(Unit) {
        val revealAt = if (returning) 1700L else 2400L
        launch { fill.animateTo(1f, tween(if (returning) 1100 else 1800, easing = FastOutSlowInEasing)) }
        launch {
            delay(revealAt - 980)
            vm.sounds.play(Sfx.FITOTV)
        }
        delay(revealAt - 400)
        textOut = true
        delay(330)
        val profiles = vm.data.value.profiles
        val next = when {
            profiles.isEmpty() -> Screen.Tutorial
            profiles.size == 1 -> {
                vm.startSession(profiles.first().id)
                Screen.Hola
            }
            else -> Screen.Profiles
        }
        vm.beginReveal(next)
    }
    SplashArt(fill.value, textAlpha, if (returning) "Hola de nuevo" else "Preparando todo")
}

/** El efecto del play: la siguiente pantalla aparece desde el triángulo del logo. */
@Composable
fun RevealOverlay(onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    val a = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { a.animateTo(0f, tween(1000, delayMillis = 200)) }
        p.animateTo(1f, tween(1250, easing = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)))
        onDone()
    }
    val small = floatArrayOf(626.9f, 264.3f, 670.7f, 287.1f, 626.9f, 312.6f)
    val big = floatArrayOf(-1110f, -2556f, 4145f, 180f, -1110f, 3240f)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val t = p.value
                val unit = size.width / 1280f
                val path = Path()
                for (i in 0 until 3) {
                    val x = (small[i * 2] + (big[i * 2] - small[i * 2]) * t) * unit
                    val y = (small[i * 2 + 1] + (big[i * 2 + 1] - small[i * 2 + 1]) * t) * unit
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Color.Black, blendMode = BlendMode.Clear)
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val z = 1f + 8f * p.value
                    scaleX = z
                    scaleY = z
                    alpha = a.value
                    transformOrigin = TransformOrigin(641.5f / 1280f, 288f / 720f)
                }
        ) {
            SplashArt(1f, 0f, "")
        }
    }
}

// ---------------------------------------------------------------------------
// Hola, Fito
// ---------------------------------------------------------------------------

@Composable
fun HolaScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val profile = vm.profile(d)
    val list = vm.activeList(d)
    LaunchedEffect(Unit) {
        delay(1800)
        vm.replaceAll(Screen.Home)
    }
    Column(
        Modifier
            .fillMaxSize()
            .splashBackground(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(180.dp)
                .border(5.dp, Fc.Menta, CircleShape)
                .padding(10.dp)
        ) {
            AvatarImage(profile?.avatar, 160.dp)
        }
        Gap(h = 36.dp)
        T("Hola, ${profile?.name ?: ""}", size = 56.sp, display = true, letterSpacing = (-1).sp)
        if (list != null) {
            Gap(h = 24.dp)
            ListChip(list.name, "${list.channelCount} canales")
        }
        Gap(h = 20.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spinner()
            T("Abriendo tu inicio", size = 17.sp, color = Fc.Muted2)
        }
    }
}

@Composable
fun ListChip(name: String, sub: String?, height: Int = 52) {
    Row(
        Modifier
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(Fc.S2)
            .padding(start = 8.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size((height - 16).dp)
                .clip(CircleShape)
                .background(Fc.Menta),
            contentAlignment = Alignment.Center,
        ) {
            T(name.take(1).uppercase(), size = 16.sp, color = Fc.Noche, weight = FontWeight.ExtraBold)
        }
        T(name, size = 19.sp, weight = FontWeight.SemiBold)
        if (sub != null) T(sub, size = 17.sp, color = Fc.Muted)
    }
}

// ---------------------------------------------------------------------------
// Tutorial
// ---------------------------------------------------------------------------

private data class Step(val title: String, val text: String)

private val steps = listOf(
    Step("Trae tus listas", "Agrega las listas que ya tienes: con un enlace, un archivo o el usuario y contraseña que te dio tu proveedor."),
    Step("Un perfil para cada quien", "Cada quien con su avatar, sus listas y sus favoritos. Los peques pueden tener un perfil solo con canales infantiles."),
    Step("Cambia de canal como siempre", "Muévete por grupos con el control, ve una vista previa al pasar por un canal y revisa qué sigue en la guía."),
)

@Composable
fun TutorialScreen(vm: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val nextFr = rememberAutoFocus()
    fun next() {
        if (step < steps.lastIndex) {
            step += 1
            vm.sounds.play(Sfx.STEP)
        } else vm.go(Screen.CreateProfile(firstTime = true))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
    ) {
        LogoFull(44.dp, Modifier.offset(56.dp, 40.dp))
        Box(Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 56.dp)) {
            SecondaryButton("Saltar", onClick = { vm.go(Screen.CreateProfile(firstTime = true)) }, height = 44.dp)
        }
        Crossfade(targetState = step, animationSpec = tween(260), label = "paso") { s ->
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .offset(96.dp, 196.dp)
                        .width(520.dp)
                ) {
                    T("Paso ${s + 1} de 3", size = 16.sp, color = Fc.Menta, weight = FontWeight.Bold)
                    Gap(h = 14.dp)
                    T(steps[s].title, size = 50.sp, display = true, maxLines = 2, lineHeight = 56.sp, letterSpacing = (-1).sp)
                    Gap(h = 18.dp)
                    T(steps[s].text, size = 21.sp, color = Fc.Muted2, maxLines = 4, lineHeight = 31.sp)
                }
                Box(
                    Modifier
                        .offset(700.dp, 150.dp)
                        .size(480.dp, 400.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (s) {
                        0 -> TutorialLists()
                        1 -> TutorialProfiles()
                        else -> TutorialChannels()
                    }
                }
            }
        }
        Row(
            Modifier.offset(96.dp, 560.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) SecondaryButton("Atrás", onClick = { step -= 1 })
            PrimaryButton(if (step < steps.lastIndex) "Siguiente" else "Crear mi perfil", onClick = { next() }, focusRequester = nextFr)
            Gap(w = 18.dp)
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .size(if (i == step) 26.dp else 10.dp, 10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (i == step) Fc.Menta else Fc.Line3)
                )
            }
        }
    }
}

@Composable
private fun TutorialLists() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(Triple("Casa de mamá", "812 canales", Fc.Menta), Triple("Deportes", "120 canales", Fc.Azul), Triple("Canales abiertos", "48 canales", Color(0xFFB79CFF)))
            .forEachIndexed { i, (name, sub, c) ->
                Row(
                    Modifier
                        .offset(x = (i * 28).dp)
                        .width(380.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (i == 0) Fc.S3 else Fc.S2)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c), contentAlignment = Alignment.Center) {
                        T(name.take(1), size = 22.sp, color = Fc.Noche, weight = FontWeight.ExtraBold)
                    }
                    Column {
                        T(name, size = 20.sp, weight = FontWeight.SemiBold)
                        T(sub, size = 15.sp, color = Fc.Muted)
                    }
                }
            }
    }
}

@Composable
private fun TutorialProfiles() {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
        listOf("audifonos" to "Fito", "pelo_largo" to "Mamá", "chongo" to "Abuela", "coletas" to "Peques").forEach { (av, name) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarImage(av, 100.dp)
                T(name, size = 18.sp, weight = FontWeight.SemiBold)
                if (av == "coletas") T("Solo infantil", size = 14.sp, color = Fc.Menta)
            }
        }
    }
}

@Composable
private fun TutorialChannels() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(420.dp, 200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Fc.S3),
        ) {
            Pill("Vista previa", bg = Color(0xD90A1747), fg = Fc.Text, fontSize = 14f, height = 30.dp)
            Box(Modifier.offset(40.dp, 50.dp).size(190.dp, 110.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B48B5)))
            Box(Modifier.offset(290.dp, 70.dp).size(70.dp).clip(CircleShape).background(Fc.Noche))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("11" to "Canal Ciudad", "12" to "Noticias 24", "13" to "Mundo Hoy").forEachIndexed { i, (n, name) ->
                Column(
                    Modifier
                        .width(132.dp)
                        .height(74.dp)
                        .then(if (i == 1) Modifier.border(3.dp, Color.White, RoundedCornerShape(14.dp)) else Modifier)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (i == 1) Fc.S3 else Fc.S2)
                        .padding(12.dp),
                ) {
                    T(n, size = 20.sp, display = true, color = Fc.Num)
                    T(name, size = 13.sp, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ¿Quién va a ver?
// ---------------------------------------------------------------------------

@Composable
fun ProfilesScreen(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    var manage by remember { mutableStateOf(false) }
    val ordered = d.profiles.sortedBy { it.createdAt }
    val focusId = ordered.firstOrNull { it.id == d.settings.lastProfileId }?.id ?: ordered.firstOrNull()?.id
    val firstFr = rememberAutoFocus()
    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
    ) {
        LogoFull(48.dp, Modifier.offset(56.dp, 40.dp))
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            T(if (manage) "Administrar perfiles" else "¿Quién va a ver?", size = 56.sp, display = true, letterSpacing = (-1).sp)
            Gap(h = 56.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.Top) {
                ordered.forEach { p ->
                    val isFirst = p.id == focusId
                    val lists = d.playlists.count { it.profileId == p.id }
                    ProfileBubble(
                        avatar = p.avatar,
                        name = p.name,
                        sub = if (p.kids) "Solo infantil" else if (lists == 1) "1 lista" else "$lists listas",
                        subAccent = p.kids,
                        editing = manage,
                        focusRequester = if (isFirst) firstFr else null,
                        onClick = {
                            if (manage) vm.go(Screen.CreateProfile(firstTime = false, editId = p.id))
                            else {
                                vm.startSession(p.id)
                                vm.replaceAll(Screen.Hola)
                            }
                        },
                    )
                }
                if (ordered.size < 6) {
                    Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FocusBox(
                            onClick = { vm.go(Screen.CreateProfile(firstTime = false)) },
                            focusRequester = if (ordered.isEmpty()) firstFr else null,
                            modifier = Modifier.size(150.dp),
                            shape = CircleShape,
                            radius = 75.dp,
                            bg = Color.Transparent,
                            focusedBg = Fc.S2,
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.fillMaxSize().border(3.dp, Color(0xFF3550A8), CircleShape), contentAlignment = Alignment.Center) {
                                Ico(Icons.PLUS, 44.dp, Fc.Num)
                            }
                        }
                        T("Agregar", size = 22.sp, color = Fc.Muted)
                    }
                }
            }
            Gap(h = 56.dp)
            SecondaryButton(if (manage) "Listo" else "Administrar perfiles", onClick = { manage = !manage }, icon = if (manage) Icons.CHECK else Icons.EDIT)
        }
        T(
            "Cada perfil guarda sus propias listas, favoritos y lo último que vio.",
            size = 16.sp,
            color = Fc.Muted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        )
    }
}

@Composable
private fun ProfileBubble(
    avatar: String,
    name: String,
    sub: String,
    subAccent: Boolean,
    editing: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(
            onClick = onClick,
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            radius = 75.dp,
            bg = Color.Transparent,
            focusedBg = Color.Transparent,
            focusRequester = focusRequester,
            onFocusChange = { focused = it },
        ) {
            AvatarImage(avatar, 150.dp)
            if (editing) {
                Box(Modifier.fillMaxSize().background(Color(0x990A1747)), contentAlignment = Alignment.Center) {
                    Ico(Icons.EDIT, 40.dp, Color.White)
                }
            }
        }
        Gap(h = 14.dp)
        T(name, size = 22.sp, weight = if (focused) FontWeight.Bold else FontWeight.Medium, color = if (focused) Color.White else Fc.Muted2)
        T(sub, size = 15.sp, color = if (subAccent) Fc.Menta else Fc.Muted)
    }
}

// ---------------------------------------------------------------------------
// Crea tu perfil
// ---------------------------------------------------------------------------

@Composable
fun CreateProfileScreen(vm: AppViewModel, s: Screen.CreateProfile) {
    val d by vm.data.collectAsState()
    val editing = d.profiles.firstOrNull { it.id == s.editId }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var avatar by remember { mutableStateOf(editing?.avatar ?: AvatarId.AUDIFONOS.key) }
    var kids by remember { mutableStateOf(editing?.kids ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nameFr = rememberAutoFocus()

    fun save() {
        when {
            editing != null -> {
                vm.updateProfile(editing.id, name, avatar, kids)
                vm.back()
            }
            s.firstTime -> {
                val id = vm.createProfile(name, avatar, kids)
                vm.startSession(id)
                vm.sounds.play(Sfx.SUCCESS)
                vm.go(Screen.AddList(firstTime = true))
            }
            else -> {
                vm.createProfile(name, avatar, kids)
                vm.sounds.play(Sfx.SUCCESS)
                vm.back()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
    ) {
        Column(Modifier.padding(start = 96.dp, top = 64.dp)) {
            T(if (editing != null) "Edita el perfil" else "Crea tu perfil", size = 48.sp, display = true, letterSpacing = (-1).sp)
            Gap(h = 10.dp)
            T("Así tus listas, favoritos y recientes quedan a tu nombre.", size = 20.sp, color = Fc.Muted)
            Gap(h = 40.dp)
            T("Tu nombre", size = 18.sp, weight = FontWeight.SemiBold)
            Gap(h = 10.dp)
            TvTextField(name, { name = it.take(20) }, "Escribe tu nombre", focusRequester = nameFr)
            Gap(h = 32.dp)
            T("Elige tu avatar", size = 18.sp, weight = FontWeight.SemiBold)
            Gap(h = 14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AvatarId.entries.forEach { a ->
                    val selected = a.key == avatar
                    FocusBox(
                        onClick = { avatar = a.key },
                        modifier = Modifier.size(104.dp),
                        shape = CircleShape,
                        radius = 52.dp,
                        bg = Color.Transparent,
                        focusedBg = Color.Transparent,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(if (selected) Modifier.border(4.dp, Fc.Menta, CircleShape) else Modifier)
                                .padding(if (selected) 8.dp else 0.dp)
                        ) {
                            AvatarImage(a.key, if (selected) 88.dp else 104.dp)
                        }
                    }
                }
            }
            Gap(h = 32.dp)
            FocusBox(
                onClick = { kids = !kids },
                modifier = Modifier
                    .width(560.dp)
                    .height(76.dp),
                radius = 18.dp,
                shape = RoundedCornerShape(18.dp),
                bg = Fc.S1,
                focusedBg = Fc.S2,
                sound = if (kids) Sfx.OFF else Sfx.ON,
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        T("Perfil infantil", size = 19.sp, weight = FontWeight.SemiBold)
                        T("Solo muestra canales del grupo Infantil", size = 15.sp, color = Fc.Muted)
                    }
                    Switch(kids)
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 96.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!s.firstTime) SecondaryButton("Atrás", onClick = { vm.back() })
            PrimaryButton(if (editing != null) "Guardar" else "Continuar", onClick = { save() })
            if (editing != null) {
                SecondaryButton(
                    if (confirmDelete) "Presiona otra vez para borrar" else "Borrar perfil",
                    icon = Icons.TRASH,
                    onClick = {
                        if (confirmDelete) {
                            vm.deleteProfile(editing.id)
                            vm.back()
                        } else confirmDelete = true
                    },
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(250.dp)
                        .border(6.dp, Fc.Menta, CircleShape)
                        .padding(14.dp)
                ) {
                    AvatarImage(avatar, 222.dp)
                }
                Gap(h = 24.dp)
                T(name.ifBlank { "Tu nombre" }, size = 34.sp, display = true, color = if (name.isBlank()) Fc.Dim else Fc.Text)
                if (kids) {
                    Gap(h = 10.dp)
                    Pill("Solo infantil")
                }
            }
        }
    }
}
