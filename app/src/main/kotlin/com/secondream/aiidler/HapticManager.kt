package com.secondream.aiidler

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

class HapticManager(private val context: Context) {
    var enabled: Boolean = true
    private val vibrator: Vibrator? = context.getSystemService()

    fun tap() {
        if (!enabled) return
        vibrate(20)
    }

    fun breakthrough() {
        if (!enabled) return
        try {
            val timings = longArrayOf(0, 90, 60, 50, 60, 110)
            val amplitudes = intArrayOf(0, 200, 0, 120, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(ms: Long) {
        try {
            val effect = VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
