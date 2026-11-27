package com.fitotv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitotv.BuildConfig
import com.fitotv.R
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import kotlinx.coroutines.delay

/**
 * Ventana obligatoria que aparece sola cuando hay una versión nueva: "Actualizar ahora" la baja y
 * abre el instalador. No se cierra con Atrás; solo si la descarga falla deja seguir por ahora.
 */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val info = vm.update ?: return
    val density = LocalDensity.current
    val sounds = LocalSounds.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        // La ventana es aparte: le pasamos la misma escala de diseño y los sonidos de la app.
        CompositionLocalProvider(LocalDensity provides density, LocalSounds provides sounds) {
            val fr = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(150)
                runCatching { fr.requestFocus() }
            }
            val prog = vm.updateProgress
            Column(
                Modifier
                    .width(680.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Fc.S1)
                    .border(2.dp, Fc.Line2, RoundedCornerShape(28.dp))
                    .padding(36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Image(painterResource(R.drawable.logo_icono), contentDescription = null, modifier = Modifier.size(64.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T("Hay una versión nueva de FitoTV", size = 28.sp, display = true)
                        T("Versión ${info.versionName} · Tienes la ${BuildConfig.VERSION_NAME}", size = 15.sp, color = Fc.Muted)
                    }
                }
                if (info.notes.isNotBlank()) {
                    T(info.notes, size = 17.sp, color = Fc.Muted2, maxLines = 3, lineHeight = 24.sp)
                }
                T("Se instala encima: tus perfiles, listas y recientes se quedan igual.", size = 15.sp, color = Fc.Muted, maxLines = 2)
                if (prog != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProgressBar(prog, 608.dp, height = 8.dp)
                        T("Descargando… ${(prog * 100).toInt()} %", size = 15.sp, weight = FontWeight.SemiBold)
                    }
                }
                vm.updateMsg?.let { msg ->
                    T(msg, size = 16.sp, color = Fc.Ambar, maxLines = 4, lineHeight = 23.sp)
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PrimaryButton(
                        when {
                            prog != null -> "Descargando…"
                            vm.updateFailed -> "Intentar de nuevo"
                            else -> "Actualizar ahora"
                        },
                        onClick = { vm.installUpdate() },
                        icon = Icons.REFRESH,
                        focusRequester = fr,
                    )
                    if (prog == null && vm.updateFailed) SecondaryButton("Seguir por ahora", onClick = { vm.postponeUpdate() })
                }
            }
        }
    }
}
