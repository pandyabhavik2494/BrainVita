package com.pandyabhavik.brainvita.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pandyabhavik.brainvita.R
import kotlin.random.Random

enum class Sound(val resId: Int) {
    PICKUP(R.raw.pickup),
    DROP(R.raw.drop),
    JUMP(R.raw.jump),
    CAPTURE(R.raw.capture),
    UNDO(R.raw.undo),
    REJECT(R.raw.reject),
    WIN(R.raw.win),
    STUCK(R.raw.stuck),
}

/** Short game sounds through a SoundPool. Clips load lazily; a clip asked for before it is decoded is skipped. */
class Sounds(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()
    private val random = Random(System.nanoTime())

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) synchronized(ready) { ready.add(sampleId) } }
        // Start decoding everything now so the first tap already has its sound.
        Sound.entries.forEach { load(it) }
    }

    private fun load(sound: Sound): Int = ids.getOrPut(sound) { pool.load(context, sound.resId, 1) }

    fun play(sound: Sound) {
        val id = load(sound)
        if (synchronized(ready) { id !in ready }) return
        // A touch of pitch variation so repeated clicks don't sound mechanical.
        val rate = if (sound == Sound.JUMP || sound == Sound.CAPTURE) 0.95f + random.nextFloat() * 0.10f else 1f
        pool.play(id, 1f, 1f, 1, 0, rate)
    }

    fun release() = pool.release()
}
