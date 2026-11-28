package com.fitotv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitotv.data.ListKind
import com.fitotv.pairing.PairedList
import com.fitotv.pairing.PairingServer
import com.fitotv.sound.Sfx
import com.fitotv.ui.AppViewModel
import com.fitotv.ui.Screen
import com.fitotv.ui.components.Gap
import com.fitotv.ui.components.Ico
import com.fitotv.ui.components.Icons
import com.fitotv.ui.components.Pill
import com.fitotv.ui.components.PrimaryButton
import com.fitotv.ui.components.Spinner
import com.fitotv.ui.components.TabChips
import com.fitotv.ui.components.TvTextField
import com.fitotv.ui.rememberAutoFocus
import com.fitotv.ui.theme.Fc
import com.fitotv.ui.theme.T
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.launch

private sealed interface AddStatus {
    data object Idle : AddStatus
    data class Checking(val name: String) : AddStatus
    data class Done(val name: String, val count: Int) : AddStatus
    data class Failed(val message: String) : AddStatus
}

@Composable
fun AddListScreen(vm: AppViewModel, s: Screen.AddList) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<AddStatus>(AddStatus.Idle) }
    var port by remember { mutableStateOf<Int?>(null) }
    val ip = remember { PairingServer.localAddress() }

    fun submit(paired: PairedList) {
        val pid = vm.profileId
        if (pid == null) {
            status = AddStatus.Failed("Primero elige o crea un perfil.")
            return
        }
        if (status is AddStatus.Checking) return
        status = AddStatus.Checking(paired.name)
        scope.launch {
            val r = vm.addList(pid, paired)
            status = r.fold(
                onSuccess = { count ->
                    vm.sounds.play(Sfx.SUCCESS)
                    AddStatus.Done(paired.name.ifBlank { "Tu lista" }, count)
                },
                onFailure = { e -> AddStatus.Failed(e.message ?: "No pudimos abrir la lista.") },
            )
        }
    }

    // Servidor para que el celular mande la lista (solo mientras esta pantalla está abierta).
    DisposableEffect(Unit) {
        var server: PairingServer? = null
        for (p in PairingServer.PORT..PairingServer.PORT + 5) {
            try {
                val srv = PairingServer(context, p) { paired -> scope.launch { submit(paired) } }
                srv.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                server = srv
                port = p
                break
            } catch (e: Exception) {
                // ese puerto está ocupado, probamos el siguiente
            }
        }
        onDispose { server?.stop() }
    }

    val address = if (ip != null && port != null) "http://$ip:$port" else null
    val qr = remember(address) { address?.let { PairingServer.qr(it, 400).asImageBitmap() } }

    var tab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val firstFr = rememberAutoFocus()

    Box(
        Modifier
            .fillMaxSize()
            .background(Fc.Noche)
    ) {
        Column(Modifier.padding(start = 64.dp, top = 48.dp, end = 64.dp)) {
            T(if (s.firstTime) "Agrega tu primera lista" else "Agrega una lista", size = 46.sp, display = true, letterSpacing = (-1).sp)
            Gap(h = 8.dp)
            T("Elige cómo. Las dos formas terminan en el mismo lugar.", size = 20.sp, color = Fc.Muted)
            Gap(h = 32.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                // Con el celular
                Column(
                    Modifier
                        .width(560.dp)
                        .height(452.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Fc.S1)
                        .padding(28.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Ico(Icons.PHONE, 26.dp, Fc.Menta)
                        T("Con tu celular", size = 24.sp, display = true)
                        Pill("Lo más fácil")
                    }
                    Gap(h = 22.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(
                            Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (qr != null) Image(qr, contentDescription = "Código QR para agregar la lista desde el celular", modifier = Modifier.size(190.dp))
                            else Spinner(32.dp, Fc.Azul)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            T("Escanea el QR con la cámara de tu celular.", size = 18.sp, weight = FontWeight.SemiBold, maxLines = 3, lineHeight = 25.sp)
                            T("Ahí pegas tu lista y aparece aquí sola.", size = 17.sp, color = Fc.Muted2, maxLines = 3, lineHeight = 24.sp)
                        }
                    }
                    Gap(h = 22.dp)
                    if (address != null) {
                        T("O abre esta dirección en el navegador:", size = 16.sp, color = Fc.Muted)
                        Gap(h = 4.dp)
                        T(address, size = 22.sp, weight = FontWeight.Bold, color = Fc.Menta)
                        Gap(h = 12.dp)
                        T("Tu celular y la tele tienen que estar en la misma red Wi-Fi.", size = 15.sp, color = Fc.Muted, maxLines = 2)
                    } else {
                        T("No encontramos la red de la tele. Revisa que esté conectada al Wi-Fi.", size = 16.sp, color = Fc.Ambar, maxLines = 2)
                    }
                }

                // Con el control
                Column(
                    Modifier
                        .width(560.dp)
                        .height(452.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Fc.S1)
                        .padding(28.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Ico(Icons.REMOTE, 26.dp, Fc.Num)
                        T("Aquí, con el control", size = 24.sp, display = true)
                    }
                    Gap(h = 4.dp)
                    T("Con el teclado de la tele, tarda un poco más.", size = 16.sp, color = Fc.Muted)
                    Gap(h = 16.dp)
                    TabChips(listOf("Enlace", "Usuario y contraseña"), tab) { tab = it }
                    Gap(h = 14.dp)
                    TvTextField(name, { name = it.take(40) }, "Nombre de la lista", width = 504.dp, focusRequester = firstFr)
                    Gap(h = 10.dp)
                    if (tab == 0) {
                        TvTextField(link, { link = it.trim() }, "Enlace: http://…/lista.m3u", width = 504.dp, keyboard = KeyboardType.Uri)
                    } else {
                        TvTextField(server, { server = it.trim() }, "Servidor: http://servidor.com:8080", width = 504.dp, keyboard = KeyboardType.Uri)
                        Gap(h = 10.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvTextField(user, { user = it.trim() }, "Usuario", width = 247.dp)
                            TvTextField(pass, { pass = it }, "Contraseña", width = 247.dp, password = true, keyboard = KeyboardType.Password)
                        }
                    }
                    Gap(h = 16.dp)
                    PrimaryButton("Agregar lista", icon = Icons.PLUS, onClick = {
                        val listName = name.ifBlank { "Mi lista" }
                        if (tab == 0) {
                            if (link.isBlank()) status = AddStatus.Failed("Falta el enlace de la lista.")
                            else submit(PairedList(listName, ListKind.URL, url = link))
                        } else {
                            if (server.isBlank() || user.isBlank() || pass.isBlank()) status = AddStatus.Failed("Faltan datos: servidor, usuario y contraseña.")
                            else submit(PairedList(listName, ListKind.XTREAM, server = server, user = user, pass = pass))
                        }
                    })
                }
            }
            if (s.firstTime) {
                Gap(h = 18.dp)
                T("Después puedes agregar más listas desde Ajustes.", size = 16.sp, color = Fc.Muted)
            }
        }

        if (status !is AddStatus.Idle) {
            StatusOverlay(
                status = status,
                onContinue = {
                    if (s.firstTime) {
                        vm.finishOnboarding()
                        vm.replaceAll(Screen.Home)
                    } else vm.back()
                },
                onRetry = {
                    status = AddStatus.Idle
                    scope.launch {
                        kotlinx.coroutines.delay(80)
                        runCatching { firstFr.requestFocus() }
                    }
                },
            )
        }
    }
}

@Composable
private fun StatusOverlay(status: AddStatus, onContinue: () -> Unit, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF20A1747)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(720.dp)) {
            when (status) {
                is AddStatus.Checking -> {
                    // Mientras revisa, el foco se queda aquí: OK no abre el teclado de los campos de atrás.
                    val hold = rememberAutoFocus(status)
                    Box(Modifier.size(1.dp).focusRequester(hold).focusable())
                    Spinner(56.dp)
                    Gap(h = 28.dp)
                    T("Revisando tu lista…", size = 40.sp, display = true)
                    Gap(h = 10.dp)
                    T("Las listas grandes tardan unos segundos.", size = 19.sp, color = Fc.Muted)
                }
                is AddStatus.Done -> {
                    val fr = rememberAutoFocus(status)
                    Box(Modifier.size(84.dp).clip(CircleShape).background(Fc.Menta), contentAlignment = Alignment.Center) {
                        Ico(Icons.CHECK, 48.dp, Fc.Noche, stroke = 2.6f)
                    }
                    Gap(h = 28.dp)
                    T("¡Listo!", size = 48.sp, display = true)
                    Gap(h = 10.dp)
                    T("${status.name} trae ${status.count} canales y programas.", size = 21.sp, color = Fc.Muted2, align = TextAlign.Center, maxLines = 2)
                    Gap(h = 32.dp)
                    PrimaryButton("Ver mis canales", onClick = onContinue, icon = Icons.PLAY, focusRequester = fr)
                }
                is AddStatus.Failed -> {
                    val fr = rememberAutoFocus(status)
                    Box(Modifier.size(84.dp).clip(CircleShape).background(Fc.Ambar), contentAlignment = Alignment.Center) {
                        Ico(Icons.WARN, 48.dp, Fc.Noche, stroke = 2.8f)
                    }
                    Gap(h = 28.dp)
                    T("No pudimos abrir la lista", size = 40.sp, display = true)
                    Gap(h = 10.dp)
                    T(status.message, size = 20.sp, color = Fc.Muted2, align = TextAlign.Center, maxLines = 3, lineHeight = 28.sp)
                    Gap(h = 32.dp)
                    PrimaryButton("Intentar otra vez", onClick = onRetry, focusRequester = fr)
                }
                AddStatus.Idle -> Unit
            }
        }
    }
}
