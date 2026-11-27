package com.fitotv.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitotv.FitoApp
import com.fitotv.data.AppData
import com.fitotv.data.Channel
import com.fitotv.data.ChannelNames
import com.fitotv.data.ContentKind
import com.fitotv.data.ListKind
import com.fitotv.data.Playlist
import com.fitotv.data.Profile
import com.fitotv.data.ProfileData
import com.fitotv.data.Programme
import com.fitotv.data.RecentItem
import com.fitotv.data.RefreshResult
import com.fitotv.data.newId
import com.fitotv.update.UpdateInfo
import com.fitotv.update.Updater
import com.fitotv.pairing.PairedList
import com.fitotv.sound.Sfx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Splash : Screen
    data object Tutorial : Screen
    data class CreateProfile(val firstTime: Boolean, val editId: String? = null) : Screen
    data class AddList(val firstTime: Boolean) : Screen
    data object Profiles : Screen
    data object Hola : Screen
    data object Home : Screen
    data object Guide : Screen
    data object Lists : Screen
    data object Groups : Screen
    data class Catalog(val kind: ContentKind) : Screen
    data object Search : Screen
    data object Settings : Screen
    data class Player(val channelId: String, val zap: List<String>, val resumeMs: Long = 0) : Screen
}

data class Toast(val title: String, val text: String, val warn: Boolean = false, val id: Long = System.nanoTime())

data class Group(val name: String, val count: Int)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as FitoApp).container
    val store = container.store
    private val repo = container.playlists
    private val epgRepo = container.epg
    val sounds = container.sounds

    val data: StateFlow<AppData> = store.data

    // ----- Navegación -----

    val stack = mutableStateListOf<Screen>(Screen.Splash)
    val screen: Screen get() = stack.last()

    fun go(s: Screen) {
        stack.add(s)
    }

    fun replaceAll(s: Screen) {
        stack.clear()
        stack.add(s)
    }

    /** Abre una sección del menú lateral: siempre queda Inicio debajo para regresar. */
    fun openTab(s: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        if (s != Screen.Home) stack.add(s)
    }

    /** Pantalla de carga: se muestra encima mientras se abre la siguiente. */
    var revealing by mutableStateOf(false)

    fun beginReveal(next: Screen) {
        replaceAll(next)
        revealing = true
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        sounds.play(Sfx.BACK)
        return true
    }

    // ----- Sesión -----

    var profileId by mutableStateOf<String?>(null)
        private set
    var channels by mutableStateOf<List<Channel>>(emptyList())
        private set
    var byId by mutableStateOf<Map<String, Channel>>(emptyMap())
        private set
    var groups by mutableStateOf<List<Group>>(emptyList())
        private set
    var epg by mutableStateOf<Map<String, List<Programme>>>(emptyMap())
        private set
    var selectedGroup by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var toast by mutableStateOf<Toast?>(null)

    // ----- Actualizaciones de la app -----

    private val updater = Updater(app)
    private var lastUpdateCheck = 0L
    private var updateFile: java.io.File? = null

    /** Versión nueva publicada en GitHub (null si ya tienes la más reciente). */
    var update by mutableStateOf<UpdateInfo?>(null)
        private set
    /** Avance de la descarga, 0 a 1; null si no se está bajando. */
    var updateProgress by mutableStateOf<Float?>(null)
        private set
    /** Mensaje para Ajustes cuando algo pide un paso tuyo. */
    var updateMsg by mutableStateOf<String?>(null)
        private set
    /** Ventana emergente "Hay una versión nueva" (se muestra fuera del reproductor). Es obligatoria. */
    var updatePrompt by mutableStateOf(false)
        private set
    /** La descarga o la instalación falló: la ventana deja reintentar o seguir por ahora. */
    var updateFailed by mutableStateOf(false)
        private set

    /** Solo después de un fallo: cierra la ventana; vuelve a salir la próxima vez que abras FitoTV. */
    fun postponeUpdate() {
        updatePrompt = false
        updateMsg = null
        updateFailed = false
    }

    /** Al volver a FitoTV (del instalador, de Ajustes del sistema o de otra app). */
    fun onAppResumed() {
        if (profileId == null) return
        if (update != null) {
            if (updateProgress == null) updatePrompt = true
        } else checkUpdate()
    }

    /** Revisa en silencio una vez por arranque; a mano desde Ajustes avisa también si estás al día. */
    fun checkUpdate(manual: Boolean = false) {
        // La versión de Amazon se actualiza por la tienda.
        if (!com.fitotv.BuildConfig.SELF_UPDATE) return
        // Las versiones de prueba (emulador) solo revisan cuando se pide en Ajustes.
        if (!manual && com.fitotv.BuildConfig.DEBUG) return
        // Las teles dejan la app abierta días: se vuelve a revisar al regresar, cada 3 horas como máximo.
        val now = System.currentTimeMillis()
        if (!manual && now - lastUpdateCheck < 3 * 60 * 60 * 1000L) return
        lastUpdateCheck = now
        viewModelScope.launch {
            if (!manual) kotlinx.coroutines.delay(8_000)
            val info = updater.check()
            update = info
            if (info != null) updatePrompt = true
            else if (manual) toast = Toast("FitoTV está al día", "Tienes la versión más nueva (${com.fitotv.BuildConfig.VERSION_NAME}).")
        }
    }

    /** Baja la versión nueva y abre el instalador. Perfiles y listas se quedan como están. */
    fun installUpdate() {
        val info = update ?: return
        if (updateProgress != null) return
        if (!updater.canInstall()) {
            val opened = updater.openInstallPermission()
            updateMsg = if (opened) {
                "Activa el permiso para FitoTV, regresa y vuelve a elegir Actualizar FitoTV."
            } else {
                "Falta un permiso: ve a Configuración, Mi Fire TV, Opciones para desarrolladores, Instalar apps desconocidas, y activa FitoTV. Luego vuelve a elegir Actualizar FitoTV."
            }
            return
        }
        viewModelScope.launch {
            updateMsg = null
            updateFailed = false
            updateProgress = 0f
            try {
                val ready = updateFile?.takeIf { it.exists() && it.name.contains(info.versionName) }
                val file = ready ?: updater.download(info) { p -> viewModelScope.launch { updateProgress = p } }
                updateFile = file
                // La ventana se queda: si cancelas el instalador, al regresar sigue ahí.
                updater.install(file)
            } catch (e: Exception) {
                updateMsg = "No se pudo bajar la actualización. Revisa tu internet e intenta de nuevo."
                updateFailed = true
            } finally {
                updateProgress = null
            }
        }
    }

    fun profile(d: AppData = data.value): Profile? = d.profiles.firstOrNull { it.id == profileId }

    fun profileData(d: AppData = data.value): ProfileData = d.profileData[profileId] ?: ProfileData()

    fun listsOf(d: AppData = data.value, pid: String? = profileId): List<Playlist> =
        d.playlists.filter { it.profileId == pid }.sortedBy { it.addedAt }

    fun activeList(d: AppData = data.value): Playlist? {
        val p = profile(d) ?: return null
        val mine = listsOf(d, p.id)
        return mine.firstOrNull { it.id == p.activeListId } ?: mine.firstOrNull()
    }

    fun startSession(id: String) {
        profileId = id
        checkUpdate()
        store.update { it.copy(settings = it.settings.copy(lastProfileId = id)) }
        viewModelScope.launch {
            loadActive()
            refreshActive(quietWhenSame = true)
        }
    }

    private suspend fun loadActive() {
        val list = activeList()
        if (list == null) {
            applyChannels(emptyList())
            epg = emptyMap()
            return
        }
        loading = true
        val all = repo.channels(list.id)
        applyChannels(all)
        epg = epgRepo.load(list.id)
        val last = profileData().lastGroup[list.id]
        selectedGroup = groups.firstOrNull { it.name == last }?.name ?: groups.firstOrNull()?.name
        loading = false
    }

    private fun applyChannels(raw: List<Channel>) {
        // Nombres limpios para pantalla ("≡P2 - Canal 5" → "Canal 5"); los id no cambian.
        val all = ChannelNames.clean(raw)
        val kids = profile()?.kids == true
        val visible = if (kids) all.filter { isKidsGroup(it.group) } else all
        channels = visible
        byId = visible.associateBy { it.id }
        val counts = LinkedHashMap<String, Int>()
        visible.forEach { if (it.kind == ContentKind.LIVE) counts[it.group] = (counts[it.group] ?: 0) + 1 }
        groups = counts.map { Group(it.key, it.value) }
    }

    fun refreshActive(quietWhenSame: Boolean = false) {
        val list = activeList() ?: return
        viewModelScope.launch {
            when (val r = repo.refresh(list)) {
                is RefreshResult.Ok -> {
                    applyChannels(repo.channels(list.id))
                    if (selectedGroup == null || groups.none { it.name == selectedGroup }) selectedGroup = groups.firstOrNull()?.name
                    when {
                        r.newLive > 0 -> toast = Toast(
                            "${list.name} está al día",
                            if (r.newLive == 1) "Llegó 1 canal nuevo." else "Llegaron ${r.newLive} canales nuevos.",
                        )
                        !quietWhenSame -> toast = Toast("${list.name} está al día", "Sin cambios desde la última vez.")
                    }
                }
                is RefreshResult.Failed -> toast = if (r.hasSavedCopy) Toast(
                    "No pudimos actualizar ${list.name}",
                    "Seguimos con la copia guardada${savedWhen(r.savedAt)}. Lo volvemos a intentar solos.",
                    warn = true,
                ) else Toast("No pudimos abrir ${list.name}", r.message, warn = true)
            }
            // La guía se actualiza cada 12 horas o si todavía no hay.
            val fresh = activeList() ?: return@launch
            val all = repo.channels(fresh.id)
            if (epg.isEmpty() || System.currentTimeMillis() - lastEpgAt > 12 * 3600_000L) {
                if (epgRepo.refresh(fresh, all)) {
                    lastEpgAt = System.currentTimeMillis()
                    epg = epgRepo.cached(fresh.id)
                }
            }
        }
    }

    private var lastEpgAt = 0L

    private fun savedWhen(at: Long): String {
        if (at <= 0) return ""
        val fmt = SimpleDateFormat("HH:mm", Locale("es", "MX"))
        val today = SimpleDateFormat("yyyyMMdd", Locale.US)
        val sameDay = today.format(Date(at)) == today.format(Date())
        return if (sameDay) " de hoy a las ${fmt.format(Date(at))}" else " del ${SimpleDateFormat("d 'de' MMMM", Locale("es", "MX")).format(Date(at))}"
    }

    fun selectGroup(name: String) {
        selectedGroup = name
        val list = activeList() ?: return
        updateProfileData { it.copy(lastGroup = it.lastGroup + (list.id to name)) }
    }

    fun channelsOf(group: String?): List<Channel> {
        if (group == FAVORITES) {
            val favs = profileData().favorites
            return favs.mapNotNull { byId[it] }
        }
        return channels.filter { it.kind == ContentKind.LIVE && (group == null || it.group == group) }
    }

    fun vod(kind: ContentKind): List<Channel> = channels.filter { it.kind == kind }

    // ----- Perfiles -----

    fun createProfile(name: String, avatar: String, kids: Boolean): String {
        val id = newId()
        store.update { d ->
            d.copy(profiles = d.profiles + Profile(id, name.trim().ifEmpty { "Yo" }, avatar, kids, createdAt = System.currentTimeMillis()))
        }
        return id
    }

    fun updateProfile(id: String, name: String, avatar: String, kids: Boolean) {
        store.update { d ->
            d.copy(profiles = d.profiles.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }, avatar = avatar, kids = kids) else it })
        }
        if (id == profileId) viewModelScope.launch { loadActive() }
    }

    fun deleteProfile(id: String) {
        val owned = data.value.playlists.filter { it.profileId == id }
        owned.forEach { repo.forget(it.id); epgRepo.forget(it.id) }
        store.update { d ->
            d.copy(
                profiles = d.profiles.filterNot { it.id == id },
                playlists = d.playlists.filterNot { it.profileId == id },
                profileData = d.profileData - id,
                settings = if (d.settings.lastProfileId == id) d.settings.copy(lastProfileId = null) else d.settings,
            )
        }
        if (profileId == id) profileId = null
    }

    fun finishOnboarding() {
        store.update { it.copy(settings = it.settings.copy(onboarded = true)) }
    }

    fun setSound(on: Boolean) {
        sounds.enabled = on
        store.update { it.copy(settings = it.settings.copy(sound = on)) }
        sounds.play(if (on) Sfx.ON else Sfx.OFF)
    }

    // ----- Listas -----

    /** Revisa y guarda una lista nueva. Devuelve cuántos canales trae o el motivo del error. */
    suspend fun addList(pid: String, paired: PairedList): Result<Int> {
        val p = Playlist(
            id = newId(),
            profileId = pid,
            name = paired.name.trim().ifEmpty { "Mi lista" },
            kind = paired.kind,
            url = paired.url.trim(),
            server = paired.server.trim(),
            user = paired.user.trim(),
            pass = paired.pass,
            addedAt = System.currentTimeMillis(),
        )
        return try {
            if (p.kind == ListKind.FILE) {
                withContext(Dispatchers.IO) { store.listFile(p.id).writeText(paired.fileText.orEmpty()) }
            }
            val result = repo.fetch(p)
            if (result.channels.isEmpty()) throw IOException("La lista no trae canales. Revisa que sea una lista M3U.")
            store.update { d ->
                d.copy(
                    playlists = d.playlists + p,
                    profiles = d.profiles.map { if (it.id == pid) it.copy(activeListId = p.id) else it },
                )
            }
            repo.commit(p, result)
            if (pid == profileId) {
                loadActive()
                refreshEpgInBackground(p.id)
            }
            Result.success(result.channels.size)
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { store.listFile(p.id).delete() }
            Result.failure(e)
        }
    }

    private fun refreshEpgInBackground(listId: String) {
        viewModelScope.launch {
            val p = data.value.playlists.firstOrNull { it.id == listId } ?: return@launch
            if (epgRepo.refresh(p, repo.channels(listId))) {
                lastEpgAt = System.currentTimeMillis()
                if (activeList()?.id == listId) epg = epgRepo.cached(listId)
            }
        }
    }

    fun setActiveList(listId: String) {
        val pid = profileId ?: return
        store.update { d -> d.copy(profiles = d.profiles.map { if (it.id == pid) it.copy(activeListId = listId) else it }) }
        viewModelScope.launch {
            loadActive()
            refreshActive(quietWhenSame = true)
        }
    }

    fun removeList(listId: String) {
        repo.forget(listId)
        epgRepo.forget(listId)
        store.update { d ->
            d.copy(
                playlists = d.playlists.filterNot { it.id == listId },
                profiles = d.profiles.map { if (it.activeListId == listId) it.copy(activeListId = null) else it },
            )
        }
        viewModelScope.launch { loadActive() }
    }

    // ----- Recientes y favoritos -----

    private fun updateProfileData(transform: (ProfileData) -> ProfileData) {
        val pid = profileId ?: return
        store.update { d -> d.copy(profileData = d.profileData + (pid to transform(d.profileData[pid] ?: ProfileData()))) }
    }

    fun recordRecent(channel: Channel, positionMs: Long = 0, durationMs: Long = 0) {
        val list = activeList() ?: return
        val item = RecentItem(channel.id, list.id, channel.kind, System.currentTimeMillis(), positionMs, durationMs)
        updateProfileData { pd ->
            // Si ya casi terminó una película o episodio, lo quitamos de "Continuar viendo".
            val finished = channel.kind != ContentKind.LIVE && durationMs > 0 && positionMs > durationMs * 0.95
            val rest = pd.recents.filterNot { it.channelId == channel.id && it.listId == list.id }
            pd.copy(recents = (if (finished) rest else listOf(item) + rest).take(60))
        }
    }

    fun recents(kindLive: Boolean): List<Pair<Channel, RecentItem>> {
        val list = activeList() ?: return emptyList()
        return profileData().recents.asSequence()
            .filter { it.listId == list.id && (it.kind == ContentKind.LIVE) == kindLive }
            .mapNotNull { r -> byId[r.channelId]?.let { it to r } }
            .take(12)
            .toList()
    }

    fun isFavorite(channelId: String): Boolean = channelId in profileData().favorites

    fun toggleFavorite(channelId: String) {
        updateProfileData { pd ->
            pd.copy(favorites = if (channelId in pd.favorites) pd.favorites - channelId else listOf(channelId) + pd.favorites)
        }
    }

    fun play(channel: Channel, zap: List<Channel>, fromStart: Boolean = false) {
        val resume = if (channel.kind == ContentKind.LIVE || fromStart) 0L else
            profileData().recents.firstOrNull { it.channelId == channel.id }?.positionMs ?: 0L
        if (channel.kind == ContentKind.LIVE) recordRecent(channel)
        go(Screen.Player(channel.id, zap.map { it.id }, resume))
    }

    fun search(query: String): List<Channel> {
        val q = fold(query.trim())
        if (q.length < 2) return emptyList()
        return channels.asSequence().filter { fold(it.name).contains(q) }.take(60).toList()
    }

    companion object {
        /** Grupo especial con los favoritos del perfil. */
        const val FAVORITES = "\u0000favoritos"

        private val kidsWords = listOf("infantil", "kids", "nino", "ninos", "children", "cartoon", "caricatura", "animad", "familia")

        fun fold(s: String): String =
            Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

        fun isKidsGroup(group: String): Boolean {
            val g = fold(group)
            return kidsWords.any { g.contains(it) }
        }
    }
}
