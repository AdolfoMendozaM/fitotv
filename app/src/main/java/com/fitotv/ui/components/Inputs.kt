package com.fitotv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.Jakarta
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay

/**
 * Campo de texto pensado para el control remoto: primero se enfoca como cualquier
 * botón y, al presionar OK, se abre el teclado de la tele. Atrás o "Listo" lo cierran.
 */
@Composable
fun TvTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    width: Dp = 460.dp,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
) {
    var editing by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    var returnFocus by remember { mutableStateOf(false) }
    val editFr = remember { FocusRequester() }
    val boxFr = focusRequester ?: remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(14.dp)

    if (!editing) {
        FocusBox(
            onClick = { editing = true; hadFocus = false },
            modifier = modifier.width(width).height(60.dp),
            shape = shape,
            radius = 14.dp,
            bg = Fc.S1,
            focusedBg = Fc.S2,
            focusRequester = boxFr,
        ) { f ->
            Row(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, if (f) Fc.Menta else Fc.Line2, shape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shown = when {
                    value.isEmpty() -> placeholder
                    password -> "•".repeat(value.length.coerceAtMost(24))
                    else -> value
                }
                T(shown, size = 20.sp, color = if (value.isEmpty()) Fc.Dim else Fc.Text)
            }
        }
        LaunchedEffect(returnFocus) {
            if (returnFocus) {
                delay(50)
                runCatching { boxFr.requestFocus() }
                returnFocus = false
            }
        }
    } else {
        BackHandler { editing = false; returnFocus = true; kb?.hide() }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Fc.Text, fontSize = 20.sp, fontFamily = Jakarta),
            cursorBrush = SolidColor(Fc.Menta),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { editing = false; returnFocus = true; kb?.hide() }),
            modifier = modifier
                .width(width)
                .height(60.dp)
                .focusRequester(editFr)
                .onFocusChanged {
                    // Si el foco se va a otro lado (cerraste el teclado con Atrás y bajaste), se deja ir:
                    // antes el campo lo volvía a jalar y no podías pasar al siguiente.
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) editing = false
                }
                .clip(shape)
                .background(Fc.S1)
                .border(2.dp, Fc.Menta, shape)
                .padding(horizontal = 18.dp, vertical = 17.dp),
        )
        LaunchedEffect(Unit) {
            delay(30)
            runCatching { editFr.requestFocus() }
            kb?.show()
        }
    }
}

/** Interruptor de encendido/apagado como el del diseño. */
@Composable
fun Switch(on: Boolean) {
    Box(
        Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) Fc.Menta else Fc.Track)
    ) {
        Box(
            Modifier
                .offset(x = if (on) 27.dp else 3.dp, y = 3.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (on) Fc.Noche else Fc.Muted2)
        )
    }
}

@Composable
fun Spinner(size: Dp = 18.dp, color: Color = Fc.Menta) {
    val t = rememberInfiniteTransition(label = "girando")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "angulo")
    Canvas(Modifier.size(size)) {
        val w = this.size.width * 0.16f
        drawArc(color, startAngle = angle, sweepAngle = 270f, useCenter = false, style = Stroke(width = w, cap = StrokeCap.Round))
    }
}

/** Botones tipo pestaña (Enlace / Usuario y contraseña…). */
@Composable
fun TabChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            FocusBox(
                onClick = { onSelect(i) },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(22.dp),
                radius = 22.dp,
                bg = if (on) Fc.Azul else Fc.S2,
                focusedBg = if (on) Fc.Azul else Fc.S3,
                onFocus = { onSelect(i) },
            ) {
                Box(Modifier.height(44.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    T(label, size = 16.sp, weight = FontWeight.SemiBold, color = if (on) Color.White else Fc.Muted2)
                }
            }
        }
    }
}
