package com.fitotv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.fitotv.ui.components.ToastHost
import com.fitotv.ui.screens.AddListScreen
import com.fitotv.ui.screens.CatalogScreen
import com.fitotv.ui.screens.CreateProfileScreen
import com.fitotv.ui.screens.GroupsScreen
import com.fitotv.ui.screens.GuideScreen
import com.fitotv.ui.screens.HolaScreen
import com.fitotv.ui.screens.HomeScreen
import com.fitotv.ui.screens.ListsScreen
import com.fitotv.ui.screens.PlayerScreen
import com.fitotv.ui.screens.ProfilesScreen
import com.fitotv.ui.screens.RevealOverlay
import com.fitotv.ui.screens.SearchScreen
import com.fitotv.ui.screens.SettingsScreen
import com.fitotv.ui.screens.SplashScreen
import com.fitotv.ui.screens.TutorialScreen
import kotlinx.coroutines.delay

@Composable
fun Root(vm: AppViewModel) {
    BackHandler(enabled = vm.stack.size > 1 && vm.screen !is Screen.Player) { vm.back() }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = vm.screen, animationSpec = tween(220), label = "pantallas") { s ->
            when (s) {
                Screen.Splash -> SplashScreen(vm)
                Screen.Tutorial -> TutorialScreen(vm)
                is Screen.CreateProfile -> CreateProfileScreen(vm, s)
                is Screen.AddList -> AddListScreen(vm, s)
                Screen.Profiles -> ProfilesScreen(vm)
                Screen.Hola -> HolaScreen(vm)
                Screen.Home -> HomeScreen(vm)
                Screen.Guide -> GuideScreen(vm)
                Screen.Lists -> ListsScreen(vm)
                Screen.Groups -> GroupsScreen(vm)
                is Screen.Catalog -> CatalogScreen(vm, s)
                Screen.Search -> SearchScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                is Screen.Player -> PlayerScreen(vm, s)
            }
        }
        if (vm.revealing) RevealOverlay(onDone = { vm.revealing = false })
        ToastHost(vm.toast, onDone = { vm.toast = null })
    }
}

/** Pide el foco para el primer elemento de una pantalla en cuanto aparece. */
@Composable
fun rememberAutoFocus(key: Any? = Unit, delayMs: Long = 80): FocusRequester {
    val fr = remember { FocusRequester() }
    LaunchedEffect(key) {
        delay(delayMs)
        runCatching { fr.requestFocus() }
    }
    return fr
}
