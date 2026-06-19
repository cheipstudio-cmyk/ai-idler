package com.secondream.aiidler

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool

class AudioManager(private val context: Context) {
    private var tapSound: Int = 0
    private var upgradeSound: Int = 0
    private var breakthroughSound: Int = 0
    
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    init {
        // Load synthetic sounds (generate on init)
        // In production, use proper audio files from res/raw/
        loadSounds()
    }

    private fun loadSounds() {
        // Tap: short beep ~200ms
        // Upgrade: ascending beep ~500ms
        // Breakthrough: deep bass + high freq ~800ms
        try {
            // Placeholder - in real app, load from res/raw/tap.ogg etc
            // tapSound = soundPool.load(context, R.raw.tap, 1)
            // upgradeSound = soundPool.load(context, R.raw.upgrade, 1)
            // breakthroughSound = soundPool.load(context, R.raw.breakthrough, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTapSound() {
        try {
            if (tapSound != 0) {
                soundPool.play(tapSound, 0.7f, 0.7f, 1, 0, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playUpgradeSound() {
        try {
            if (upgradeSound != 0) {
                soundPool.play(upgradeSound, 1f, 1f, 1, 0, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playBreakthroughSound() {
        try {
            if (breakthroughSound != 0) {
                soundPool.play(breakthroughSound, 1f, 1f, 2, 0, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        soundPool.release()
    }
}
