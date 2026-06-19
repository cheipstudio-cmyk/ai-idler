package com.secondream.aiidler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private lateinit var gameLogic: GameLogic
    private lateinit var audioManager: AudioManager
    private lateinit var hapticManager: HapticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        gameLogic = GameLogic()
        audioManager = AudioManager(this)
        hapticManager = HapticManager(this)

        setContent {
            val darkTheme = darkColorScheme(
                primary = Color(0xFF4A9EFF),
                secondary = Color(0xFFD9A85C),
                tertiary = Color(0xFF00FF88),
                background = Color(0xFF0B0B0D),
                surface = Color(0xFF1A1A1F)
            )

            MaterialTheme(colorScheme = darkTheme) {
                Surface {
                    GameScreen(
                        gameLogic = gameLogic,
                        audioManager = audioManager,
                        hapticManager = hapticManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
