package com.secondream.aiidler

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

class HapticManager(private val context: Context) {
    private val vibrator: Vibrator? = context.getSystemService()

    fun tap() {
        vibrate(20) // Light 20ms tap
    }

    fun breakthrough() {
        vibrate(100)
        Thread.sleep(100)
        vibrate(50)
        Thread.sleep(50)
        vibrate(100) // Double pulse
    }

    private fun vibrate(ms: Long) {
        try {
            val effect = VibrationEffect.createOneShot(
                ms,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
