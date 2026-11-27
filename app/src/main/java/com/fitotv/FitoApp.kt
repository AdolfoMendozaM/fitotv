package com.fitotv

import android.app.Application
import android.content.Context
import com.fitotv.data.EpgRepository
import com.fitotv.data.PlaylistRepository
import com.fitotv.data.Store
import com.fitotv.sound.Sounds

class AppContainer(context: Context) {
    val store = Store(context.filesDir)
    val playlists = PlaylistRepository(store)
    val epg = EpgRepository(store)
    val sounds = Sounds(context).apply { enabled = store.data.value.settings.sound }
}

class FitoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
