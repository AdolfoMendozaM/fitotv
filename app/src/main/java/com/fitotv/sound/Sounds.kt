package com.fitotv.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.fitotv.R

enum class Sfx { TICK, OK, BACK, STEP, ON, OFF, SUCCESS, FITOTV }

/** Los sonidos de marimba de FitoTV. Se pueden apagar en Ajustes. */
class Sounds(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<Sfx, Int> = mapOf(
        Sfx.TICK to pool.load(context, R.raw.sfx_tick, 1),
        Sfx.OK to pool.load(context, R.raw.sfx_ok, 1),
        Sfx.BACK to pool.load(context, R.raw.sfx_back, 1),
        Sfx.STEP to pool.load(context, R.raw.sfx_step, 1),
        Sfx.ON to pool.load(context, R.raw.sfx_on, 1),
        Sfx.OFF to pool.load(context, R.raw.sfx_off, 1),
        Sfx.SUCCESS to pool.load(context, R.raw.sfx_success, 1),
        Sfx.FITOTV to pool.load(context, R.raw.sfx_fitotv, 1),
    )

    @Volatile
    var enabled: Boolean = true

    private var lastTick = 0L

    fun play(sfx: Sfx) {
        if (!enabled) return
        if (sfx == Sfx.TICK) {
            val now = System.currentTimeMillis()
            if (now - lastTick < 60) return
            lastTick = now
        }
        val volume = when (sfx) {
            Sfx.TICK -> 0.35f
            Sfx.FITOTV -> 0.9f
            else -> 0.6f
        }
        ids[sfx]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }
}
