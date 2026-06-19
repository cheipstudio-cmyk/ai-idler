package com.secondream.aiidler

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

class MainActivity : ComponentActivity() {
    private lateinit var gameLogic: GameLogic
    private lateinit var audioManager: AudioManager
    private lateinit var hapticManager: HapticManager
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("ai_idler", Context.MODE_PRIVATE)
        gameLogic = GameLogic()
        audioManager = AudioManager(this)
        hapticManager = HapticManager(this)

        audioManager.enabled = prefs.getBoolean("sound", true)
        hapticManager.enabled = prefs.getBoolean("haptics", true)

        val offlineGain = gameLogic.loadFrom(prefs)
        val startOnboarding = !prefs.getBoolean("onboarded", false)

        setContent {
            val scheme = darkColorScheme(
                primary = Color(0xFF4A9EFF),
                secondary = Color(0xFFD9A85C),
                tertiary = Color(0xFF00FF88),
                background = Color(0xFF0B0B0D),
                surface = Color(0xFF1A1A1F)
            )

            MaterialTheme(colorScheme = scheme) {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(fontFamily = Rajdhani, color = Color.White)
                ) {
                    Surface {
                        GameScreen(
                            gameLogic = gameLogic,
                            audioManager = audioManager,
                            hapticManager = hapticManager,
                            offlineGain = offlineGain,
                            startOnboarding = startOnboarding,
                            onSave = { gameLogic.saveTo(prefs) },
                            onFinishOnboarding = { prefs.edit().putBoolean("onboarded", true).apply() },
                            onToggleSound = { prefs.edit().putBoolean("sound", it).apply() },
                            onToggleHaptics = { prefs.edit().putBoolean("haptics", it).apply() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        gameLogic.saveTo(prefs)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
