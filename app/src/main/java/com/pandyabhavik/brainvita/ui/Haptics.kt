package com.pandyabhavik.brainvita.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptics for the marbles, played straight on the vibrator with game attributes, so they work even when
 * the system "touch feedback" setting is off. Phones with a good motor get composed primitives (a soft
 * thud when a marble lands, a tick when it is lifted); older ones get the closest predefined effect.
 */
class Haptics(context: Context) {
    enum class Pattern { PICK_UP, PUT_DOWN, JUMP, UNDO, REJECT, WIN, STUCK }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val composable: Boolean = vibrator != null && Build.VERSION.SDK_INT >= 31 && vibrator.areAllPrimitivesSupported(
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_THUD,
        VibrationEffect.Composition.PRIMITIVE_CLICK,
    )

    fun play(pattern: Pattern) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (composable) composed(pattern) else fallback(pattern)
        vibrate(v, effect)
    }

    private fun vibrate(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_GAME))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
    }

    private fun composed(pattern: Pattern): VibrationEffect {
        val c = VibrationEffect.startComposition()
        val lowTick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        val tick = VibrationEffect.Composition.PRIMITIVE_TICK
        val thud = VibrationEffect.Composition.PRIMITIVE_THUD
        val click = VibrationEffect.Composition.PRIMITIVE_CLICK
        when (pattern) {
            Pattern.PICK_UP -> c.addPrimitive(lowTick, 0.5f)
            Pattern.PUT_DOWN -> c.addPrimitive(tick, 0.4f)
            // The marble settles with a thud; a moment later the captured one is lifted away.
            Pattern.JUMP -> c.addPrimitive(thud, 0.9f).addPrimitive(lowTick, 0.3f, 60)
            Pattern.UNDO -> c.addPrimitive(tick, 0.5f).addPrimitive(tick, 0.3f, 70)
            Pattern.REJECT -> c.addPrimitive(lowTick, 0.6f).addPrimitive(lowTick, 0.6f, 90)
            Pattern.WIN -> c.addPrimitive(thud, 0.6f).addPrimitive(click, 0.5f, 120).addPrimitive(thud, 1.0f, 180)
            Pattern.STUCK -> c.addPrimitive(thud, 0.7f).addPrimitive(thud, 0.4f, 160)
        }
        return c.compose()
    }

    private fun fallback(pattern: Pattern): VibrationEffect = when (pattern) {
        Pattern.PICK_UP -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        Pattern.PUT_DOWN -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        Pattern.JUMP -> VibrationEffect.createWaveform(longArrayOf(0, 45, 40, 20), intArrayOf(0, 200, 0, 90), -1)
        Pattern.UNDO -> VibrationEffect.createWaveform(longArrayOf(0, 20, 50, 15), intArrayOf(0, 120, 0, 80), -1)
        Pattern.REJECT -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30), intArrayOf(0, 150, 0, 150), -1)
        Pattern.WIN -> VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 25, 120, 70), intArrayOf(0, 140, 0, 110, 0, 255), -1)
        Pattern.STUCK -> VibrationEffect.createWaveform(longArrayOf(0, 50, 110, 40), intArrayOf(0, 180, 0, 100), -1)
    }
}
