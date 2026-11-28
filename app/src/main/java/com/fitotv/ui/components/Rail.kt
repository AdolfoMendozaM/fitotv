package com.fitotv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.data.ContentKind
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T

enum class Tab { HOME, GUIDE, MOVIES, SERIES, SEARCH, SETTINGS }

private data class NavItem(val tab: Tab, val label: String, val icon: String, val screen: Screen)

private val navItems = listOf(
    NavItem(Tab.HOME, "Inicio", Icons.HOME, Screen.Home),
    NavItem(Tab.GUIDE, "Guía", Icons.GUIDE, Screen.Guide),
    NavItem(Tab.MOVIES, "Películas", Icons.FILM, Screen.Catalog(ContentKind.MOVIE)),
    NavItem(Tab.SERIES, "Series", Icons.TV, Screen.Catalog(ContentKind.SERIES)),
    NavItem(Tab.SEARCH, "Buscar", Icons.SEARCH, Screen.Search),
)

/**
 * Menú lateral de las pantallas principales, como en el diseño.
 *
 * El menú solo se activa con la flecha izquierda: subir o bajar dentro de la pantalla nunca salta
 * al menú (antes, al llegar arriba o abajo de una lista, el foco se iba al avatar o a "Guía").
 * Al entrar al menú el foco cae en la sección donde estás, y al volver a la derecha regresa a
 * lo último que tenías elegido.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScaffold(vm: AppViewModel, tab: Tab, content: @Composable BoxScope.() -> Unit) {
    val d by vm.data.collectAsState()
    val profile = vm.profile(d)
    val current = remember { FocusRequester() }
    Row(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
    ) {
        Column(
            Modifier
                .width(96.dp)
                .fillMaxHeight()
                .background(Fc.Panel)
                .border(width = 1.dp, color = Fc.Line, shape = RoundedCornerShape(0.dp))
                .focusProperties { enter = { current } }
                .focusGroup()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LogoIcon(44.dp)
            Gap(h = 14.dp)
            FocusBox(
                onClick = { vm.go(Screen.Profiles) },
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                radius = 26.dp,
                bg = Color.Transparent,
                focusedBg = Color.Transparent,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(3.dp, Fc.Menta, CircleShape)
                ) {
                    AvatarImage(profile?.avatar, 52.dp)
                }
            }
            Gap(h = 26.dp)
            navItems.forEach { item ->
                RailButton(item.label, item.icon, selected = item.tab == tab, fr = if (item.tab == tab) current else null) {
                    if (item.tab != tab) vm.openTab(item.screen)
                }
            }
            Box(Modifier.weight(1f))
            RailButton("Ajustes", Icons.SETTINGS, selected = tab == Tab.SETTINGS, fr = if (tab == Tab.SETTINGS) current else null) {
                if (tab != Tab.SETTINGS) vm.openTab(Screen.Settings)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusProperties {
                    exit = { dir ->
                        if (dir == FocusDirection.Up || dir == FocusDirection.Down) FocusRequester.Cancel else FocusRequester.Default
                    }
                }
                .focusRestorer()
                .focusGroup()
        ) {
            content()
        }
    }
}

@Composable
private fun RailButton(label: String, icon: String, selected: Boolean, fr: FocusRequester? = null, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        focusRequester = fr,
        modifier = Modifier.size(72.dp, 64.dp),
        bg = if (selected) Fc.Azul else Color.Transparent,
        focusedBg = if (selected) Fc.Azul else Fc.S3,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Ico(icon, 24.dp, if (selected) Color.White else Fc.Muted)
            T(label, size = 12.sp, weight = FontWeight.SemiBold, color = if (selected) Color.White else Fc.Muted)
        }
    }
}

/** Pantalla vacía con una acción clara (sin lista, sin canales, sin resultados…). */
@Composable
fun EmptyState(title: String, text: String, action: String?, onAction: (() -> Unit)?, icon: String = Icons.LIST) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .background(Fc.S2, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Ico(icon, 40.dp, Fc.Num) }
        Gap(h = 24.dp)
        T(title, size = 34.sp, display = true)
        Gap(h = 10.dp)
        T(text, size = 19.sp, color = Fc.Muted, maxLines = 3)
        if (action != null && onAction != null) {
            Gap(h = 28.dp)
            val fr = com.fitotv.ui.rememberAutoFocus()
            PrimaryButton(action, onClick = onAction, icon = Icons.PLUS, focusRequester = fr)
        }
    }
}

@Composable
fun Crumb(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        T(text, size = 14.sp, color = Fc.Muted)
    }
}
