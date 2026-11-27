package com.fitotv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Root
import com.fitotv.ui.components.LocalSounds
import com.fitotv.ui.theme.Fc

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val vm: AppViewModel = viewModel()
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(Fc.Noche)
            ) {
                // Todas las pantallas se diseñaron a 1280 de ancho: así se ven igual en
                // una tele de 720p, de 1080p o de 4K.
                val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val design = Density(density = widthPx / 1280f, fontScale = 1f)
                CompositionLocalProvider(LocalDensity provides design, LocalSounds provides vm.sounds) {
                    Root(vm)
                }
            }
        }
    }
}
