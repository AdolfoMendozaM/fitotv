package com.fitotv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.fitotv.ui.components.ToastHost
import com.fitotv.ui.components.UpdateDialog
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
        // Cada pantalla guarda su desplazamiento: al regresar de un canal o una película queda igual.
        val saved = rememberSaveableStateHolder()
        Crossfade(targetState = vm.screen, animationSpec = tween(220), label = "pantallas") { s ->
            saved.SaveableStateProvider(screenKey(s)) {
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
                is Screen.Series -> com.fitotv.ui.screens.SeriesScreen(vm, s)
                Screen.Settings -> SettingsScreen(vm)
                is Screen.Player -> PlayerScreen(vm, s)
            }
            }
        }
        if (vm.revealing) RevealOverlay(onDone = { vm.revealing = false })
        ToastHost(vm.toast, onDone = { vm.toast = null })
        // Versión nueva: ventana emergente en las pantallas principales, nunca encima de un canal.
        val main = when (vm.screen) {
            Screen.Home, Screen.Guide, Screen.Lists, Screen.Groups, Screen.Search, Screen.Settings, is Screen.Catalog, is Screen.Series -> true
            else -> false
        }
        if (vm.updatePrompt && vm.update != null && main && !vm.revealing) UpdateDialog(vm)
    }
}

fun screenKey(s: Screen): String = when (s) {
    is Screen.Catalog -> "catalog-${s.kind}"
    is Screen.Series -> "series-${s.showId}"
    is Screen.Player -> "player"
    is Screen.CreateProfile -> "profile-${s.editId}"
    is Screen.AddList -> "addlist"
    else -> s.toString()
}

/** Primer elemento de la pantalla y, si regresas, el elemento que habías elegido. */
class EntryFocus(val first: FocusRequester, val back: FocusRequester, val backKey: String?) {
    /** El FocusRequester que le toca a un elemento: el que elegiste antes, o el primero. */
    fun of(itemKey: String, isFirst: Boolean): FocusRequester? = when {
        backKey != null && itemKey == backKey -> back
        isFirst -> first
        else -> null
    }
}

/**
 * Al entrar a una pantalla: si vienes de regreso (de un canal, película o serie), el foco vuelve a
 * lo que elegiste; si no, va al primer elemento.
 */
@Composable
fun rememberEntryFocus(vm: AppViewModel, screenKey: String, key: Any? = Unit): EntryFocus {
    val ef = remember(screenKey) { EntryFocus(FocusRequester(), FocusRequester(), vm.returnTo.remove(screenKey)) }
    LaunchedEffect(screenKey, key) {
        delay(80)
        if (ef.backKey != null) {
            // Lo elegido puede tardar en aparecer (temporadas que se piden, búsquedas): se espera un poco.
            repeat(25) {
                if (runCatching { ef.back.requestFocus() }.isSuccess) return@LaunchedEffect
                delay(60)
            }
        }
        runCatching { ef.first.requestFocus() }
    }
    return ef
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
