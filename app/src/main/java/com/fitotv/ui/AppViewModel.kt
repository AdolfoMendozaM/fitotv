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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Splash : Screen
    data object Tutorial : Screen
    data class CreateProfile(val firstTime: Boolean, val editId: String? = null) : Screen
    data class AddList(val firstTime: Boolean, val editId: String? = null) : Screen
    data object Profiles : Screen
    data object Hola : Screen
    data object Home : Screen
    data object Guide : Screen
    data object Lists : Screen
    data object Groups : Screen
    data class Catalog(val kind: ContentKind) : Screen
    data object Search : Screen
    data class Series(val showId: String) : Screen
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
    /** Todo lo de la lista activa ya ordenado e indexado (se arma fuera del hilo de la pantalla). */
    var lib by mutableStateOf(Library.EMPTY)
        private set
    val channels: List<Channel> get() = lib.all
    val byId: Map<String, Channel> get() = lib.byId
    val groups: List<Group> get() = lib.groups

    /** Episodios que no vienen en la lista guardada (series de Xtream, se piden al abrirlas). */
    private val extra = java.util.concurrent.ConcurrentHashMap<String, Channel>()

    fun channel(id: String): Channel? = lib.byId[id] ?: extra[id]

    /**
     * Lo que elegiste en cada pantalla ("home", "catalog-MOVIE", "series-…"), para que al regresar
     * de un canal, una película o una serie el foco vuelva a ese mismo lugar y no al principio.
     */
    val returnTo = HashMap<String, String>()

    fun markReturn(screenKey: String, itemKey: String) {
        returnTo[screenKey] = itemKey
    }
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
            refreshIfStale()
        }
    }

    /**
     * Al abrir FitoTV se usa la copia guardada (abre al instante) y la lista se vuelve a bajar
     * en segundo plano solo si tiene más de 6 horas. Antes se bajaba completa en cada arranque.
     */
    private fun refreshIfStale() {
        val list = activeList() ?: return
        val age = System.currentTimeMillis() - list.updatedAt
        if (lib.all.isEmpty() || list.updatedAt == 0L || age > 6 * 3600_000L) {
            refreshActive(quietWhenSame = true)
        } else {
            refreshEpgIfOld(list)
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
        lastEpgAt = epgRepo.savedAt(list.id)
        val last = profileData().lastGroup[list.id]
        val lastClean = last?.let { com.fitotv.data.NameText.cleanGroup(it) }
        selectedGroup = if (last == FAVORITES && profileData().favorites.isNotEmpty()) FAVORITES
        else groups.firstOrNull { it.name == last || it.name == lastClean }?.name ?: groups.firstOrNull()?.name
        loading = false
    }

    /** Los nombres ya vienen limpios desde que se bajó la lista; aquí solo se indexa, fuera de la pantalla. */
    private suspend fun applyChannels(raw: List<Channel>) {
        val kids = profile()?.kids == true
        lib = withContext(Dispatchers.Default) { Library.build(raw, kids) }
        // Proveedores enormes de Xtream: la guía completa (XMLTV) pesa cientos de megas y llenaría la
        // memoria; se pide canal por canal lo que está en pantalla.
        val list = activeList()
        val creds = when (list?.kind) {
            ListKind.XTREAM -> com.fitotv.data.XtreamApi.Creds(list.server, list.user, list.pass)
            ListKind.URL -> com.fitotv.data.XtreamApi.credsFromUrl(list.url)
            else -> null
        }
        shortEpgCreds = if (creds != null && lib.live.size > 2500) creds else null
        epgRequested.clear()
    }

    // ----- Guía por canal (listas enormes de Xtream) -----

    private var shortEpgCreds: com.fitotv.data.XtreamApi.Creds? = null
    private val epgRequested = HashSet<String>()

    /**
     * Con calma para no molestar al proveedor (algunos bloquean un rato a quien pide mucho de golpe):
     * una petición a la vez, con una pausa entre cada una, máximo 120 cada 10 minutos, y si el
     * servidor contesta con error se deja de pedir guía 10 minutos.
     */
    private val epgMutex = kotlinx.coroutines.sync.Mutex()
    private val epgTimes = ArrayDeque<Long>()
    private var epgPausedUntil = 0L

    /** Lo que pasa ahora y después; en listas enormes lo pide al proveedor la primera vez. */
    fun epgFor(ch: Channel): Pair<Programme?, Programme?> {
        wantEpg(ch)
        return com.fitotv.ui.screens.epgFor(epg, ch)
    }

    fun wantEpg(ch: Channel) {
        val creds = shortEpgCreds ?: return
        if (ch.kind != ContentKind.LIVE || ch.tvgId.isBlank()) return
        if (System.currentTimeMillis() < epgPausedUntil) return
        val key = ch.tvgId.lowercase()
        if (epg.containsKey(key) || !epgRequested.add(key)) return
        val sid = ch.url.substringBefore('?').substringAfterLast('/').substringBefore('.')
        if (sid.isEmpty() || !sid.all { it.isDigit() }) return
        viewModelScope.launch {
            val progs = epgMutex.withLock {
                val now = System.currentTimeMillis()
                while (epgTimes.isNotEmpty() && now - epgTimes.first() > 10 * 60_000L) epgTimes.removeFirst()
                if (now < epgPausedUntil || epgTimes.size >= 120) {
                    epgRequested.remove(key)
                    return@withLock null
                }
                epgTimes.addLast(now)
                val r = try {
                    withContext(Dispatchers.IO) { com.fitotv.data.XtreamApi.shortEpg(creds, sid) }
                } catch (e: Exception) {
                    epgPausedUntil = System.currentTimeMillis() + 10 * 60_000L
                    epgRequested.remove(key)
                    null
                }
                kotlinx.coroutines.delay(350)
                r
            } ?: return@launch
            if (progs.isNotEmpty() && shortEpgCreds == creds) epg = epg + (key to progs)
        }
    }

    fun refreshActive(quietWhenSame: Boolean = false) {
        val list = activeList() ?: return
        viewModelScope.launch {
            when (val r = repo.refresh(list)) {
                is RefreshResult.Ok -> {
                    if (r.remap.isNotEmpty()) keepFavoritesAndRecents(list.id, r.remap)
                    if (activeList()?.id != list.id) return@launch
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
            activeList()?.let { refreshEpgIfOld(it) }
        }
    }

    /**
     * La guía se actualiza cada 12 horas o si todavía no hay; en segundo plano y con calma. Las
     * listas enormes piden la guía del proveedor canal por canal, pero también usan la pública.
     */
    private fun refreshEpgIfOld(list: Playlist) {
        val fresh = list.epgVersion >= EPG_VERSION && lastEpgAt > 0 && System.currentTimeMillis() - lastEpgAt < 12 * 3600_000L
        if (fresh && (epg.isNotEmpty() || shortEpgCreds != null)) return
        viewModelScope.launch {
            // Que primero se acomode la pantalla.
            kotlinx.coroutines.delay(15_000)
            if (activeList()?.id != list.id) return@launch
            updateEpg(list, lib.live)
        }
    }

    private var epgRunning = false

    private suspend fun updateEpg(list: Playlist, channels: List<Channel>) {
        if (epgRunning) return
        epgRunning = true
        try {
            val short = shortEpgCreds != null
            if (epgRepo.refresh(list, channels, providerGuide = !short)) {
                lastEpgAt = System.currentTimeMillis()
                store.update { d -> d.copy(playlists = d.playlists.map { if (it.id == list.id) it.copy(epgVersion = EPG_VERSION) else it }) }
                if (activeList()?.id == list.id) epg = if (short) epg + epgRepo.cached(list.id) else epgRepo.cached(list.id)
            }
        } finally {
            epgRunning = false
        }
    }

    private var lastEpgAt = 0L

    /** Los favoritos y lo que estabas viendo siguen ahí aunque el canal haya cambiado de id. */
    private fun keepFavoritesAndRecents(listId: String, remap: Map<String, String>) {
        store.update { d ->
            d.copy(profileData = d.profileData.mapValues { (_, pd) ->
                pd.copy(
                    favorites = pd.favorites.map { remap[it] ?: it }.distinct(),
                    recents = pd.recents.map { if (it.listId == listId) it.copy(channelId = remap[it.channelId] ?: it.channelId) else it },
                )
            })
        }
    }

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
            return favs.mapNotNull { lib.byId[it] }.filter { it.kind == ContentKind.LIVE }
        }
        if (group == null) return lib.live
        return lib.liveByGroup[group] ?: emptyList()
    }

    /** Películas o series (una entrada por serie) con sus categorías. */
    fun catalog(kind: ContentKind): Catalog = if (kind == ContentKind.MOVIE) lib.movies else lib.shows

    /** Temporadas y episodios de una serie: del proveedor (Xtream) o de los episodios sueltos de la lista. */
    suspend fun seriesInfo(show: Channel): Result<com.fitotv.data.XtreamApi.SeriesInfo> {
        val list = activeList() ?: return Result.failure(IOException("No hay una lista abierta"))
        return try {
            val info = if (show.seriesId.startsWith("x:")) {
                repo.seriesInfo(list, show)
            } else {
                val eps = lib.episodesByShow[show.seriesId].orEmpty()
                com.fitotv.data.XtreamApi.SeriesInfo(
                    plot = "",
                    cover = show.logo,
                    year = "",
                    seasons = eps.groupBy { it.season }.toSortedMap().map { (n, list) ->
                        com.fitotv.data.XtreamApi.Season(n, if (n > 0) "Temporada $n" else "Episodios", list)
                    },
                )
            }
            info.seasons.forEach { s -> s.episodes.forEach { e -> if (lib.byId[e.id] == null) extra[e.id] = e } }
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            updateEpg(p, repo.channels(listId))
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
        // Los episodios de Xtream no están en la lista guardada: se guarda una copia para "Continuar viendo".
        val snap = if (lib.byId[channel.id] == null) channel else null
        val item = RecentItem(channel.id, list.id, channel.kind, System.currentTimeMillis(), positionMs, durationMs, snap)
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
            .mapNotNull { r ->
                val ch = lib.byId[r.channelId] ?: r.snapshot?.also { extra[it.id] = it }
                ch?.let { it to r }
            }
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

    suspend fun search(query: String, kind: ContentKind? = null): List<Channel> {
        val l = lib
        return withContext(Dispatchers.Default) { l.search(query, kind = kind) }
    }

    // ----- Cuenta del proveedor -----

    /** Vuelve a preguntar al proveedor el vencimiento y las pantallas (al abrir Ajustes, máximo cada hora). */
    fun refreshAccount(force: Boolean = false) {
        val list = activeList() ?: return
        if (repo.credsOf(list) == null) return
        if (!force && System.currentTimeMillis() - list.accountCheckedAt < 3600_000L) return
        viewModelScope.launch { repo.account(list) }
    }

    fun hasAccountInfo(list: Playlist): Boolean = repo.credsOf(list) != null

    // ----- Editar una lista -----

    /**
     * Guarda los cambios de una lista. Si solo cambia el nombre se guarda al instante; si cambian el
     * enlace o los datos de la cuenta, primero se revisa que abra y luego se reemplaza.
     */
    suspend fun editList(listId: String, paired: PairedList): Result<Int> {
        val old = data.value.playlists.firstOrNull { it.id == listId } ?: return Result.failure(IOException("Esa lista ya no existe"))
        val name = paired.name.trim().ifEmpty { old.name }
        val updated = old.copy(
            name = name,
            kind = paired.kind,
            url = if (paired.kind == ListKind.URL) paired.url.trim() else "",
            server = if (paired.kind == ListKind.XTREAM) paired.server.trim() else "",
            user = if (paired.kind == ListKind.XTREAM) paired.user.trim() else "",
            pass = if (paired.kind == ListKind.XTREAM) paired.pass.ifEmpty { old.pass } else "",
            epgUrl = if (paired.kind == old.kind && paired.url.trim() == old.url && paired.server.trim() == old.server) old.epgUrl else "",
        )
        val sameSource = updated.kind == old.kind && updated.url == old.url && updated.server == old.server &&
            updated.user == old.user && updated.pass == old.pass
        if (sameSource || old.kind == ListKind.FILE && paired.kind == ListKind.FILE) {
            store.update { d -> d.copy(playlists = d.playlists.map { if (it.id == listId) it.copy(name = name) else it }) }
            return Result.success(old.channelCount)
        }
        return try {
            val before = repo.channels(listId)
            val result = repo.fetch(updated)
            if (result.channels.isEmpty()) throw IOException("La lista no trae canales. Revisa los datos.")
            store.update { d -> d.copy(playlists = d.playlists.map { if (it.id == listId) updated.copy(lastError = null) else it }) }
            repo.commit(updated, result)
            val remap = HashMap<String, String>()
            // Mismo canal con otro id (por ejemplo, de enlace a usuario y contraseña): favoritos y recientes siguen.
            val newIds = result.channels.mapTo(HashSet()) { it.id }
            val byKey = HashMap<String, String>()
            result.channels.forEach { c -> streamKey(c)?.let { byKey.putIfAbsent(it, c.id) } }
            before.forEach { o -> if (o.id !in newIds) streamKey(o)?.let { byKey[it] }?.let { remap[o.id] = it } }
            if (remap.isNotEmpty()) keepFavoritesAndRecents(listId, remap)
            if (activeList()?.id == listId) {
                loadActive()
                refreshEpgInBackground(listId)
            }
            Result.success(result.channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun streamKey(c: Channel): String? {
        val last = c.url.substringBefore('?').substringAfterLast('/').substringBefore('.')
        if (last.isEmpty() || !last.all { it.isDigit() }) return null
        return c.kind.name + ":" + last
    }

    // ----- Subtítulos e idioma -----

    fun setTrackPrefs(audioLang: String? = null, subLang: String? = null) {
        store.update { d ->
            d.copy(settings = d.settings.copy(
                audioLang = audioLang ?: d.settings.audioLang,
                subLang = subLang ?: d.settings.subLang,
            ))
        }
    }

    companion object {
        /** Grupo especial con los favoritos del perfil. */
        const val FAVORITES = "\u0000favoritos"

        /** Versión de la guía guardada: 2 ya incluye la guía pública buscada por nombre. */
        const val EPG_VERSION = 2

        fun fold(s: String): String = com.fitotv.data.Fold.fold(s)

        fun isKidsGroup(group: String): Boolean = com.fitotv.data.Fold.isKidsGroup(group)
    }
}
