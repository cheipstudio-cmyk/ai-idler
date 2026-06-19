package com.secondream.aiidler

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.floor
import kotlin.math.pow

data class UpgradeInfo(
    val id: String,
    val name: String,
    val baseCost: Long,
    val costMultiplier: Double = 1.15,
    val owned: Int = 0,
    val effect: Double = 1.0
)

data class GameState(
    val gpu: Long = 0,
    val totalTaps: Long = 0,
    val level: Int = 1,
    val workers: Int = 0,
    val multiplier: Double = 1.0,
    val prestige: Int = 0,
    val upgrades: Map<String, UpgradeInfo> = defaultUpgrades()
) {
    val prestigeBonus: Double get() = 1.0 + prestige * 0.25
    val incomePerSec: Long get() = (workers * 10 * prestigeBonus).toLong()
    val levelTarget: Long get() = level * 100_000L
    val canPrestige: Boolean get() = gpu >= PRESTIGE_THRESHOLD

    companion object {
        const val PRESTIGE_THRESHOLD = 1_000_000L

        fun defaultUpgrades(): Map<String, UpgradeInfo> = mapOf(
            "gpu_farm" to UpgradeInfo("gpu_farm", "GPU Farm", 10, effect = 1.0),
            "worker_bot" to UpgradeInfo("worker_bot", "Worker Bot", 50, effect = 1.0),
            "processing_core" to UpgradeInfo("processing_core", "Processing Core", 150, effect = 1.5),
            "ai_booster" to UpgradeInfo("ai_booster", "AI Booster", 500, effect = 2.0)
        )
    }
}

class GameLogic {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private var fractional = 0.0

    fun tap(): Pair<Long, Boolean> {
        val s = _gameState.value
        val farmOwned = s.upgrades["gpu_farm"]?.owned ?: 0
        val baseGain = 1L + (s.multiplier * farmOwned).toLong()
        val totalGain = (baseGain * 100 * s.prestigeBonus).toLong()

        val newGpu = s.gpu + totalGain
        val breakthrough = newGpu > s.levelTarget

        _gameState.value = s.copy(
            gpu = newGpu,
            totalTaps = s.totalTaps + 1,
            level = if (breakthrough) s.level + 1 else s.level,
            multiplier = if (breakthrough) s.multiplier * 1.1 else s.multiplier
        )
        return Pair(totalGain, breakthrough)
    }

    /** Idle income while the app is open. Call frequently with elapsed seconds. */
    fun accrue(seconds: Double) {
        val s = _gameState.value
        if (s.incomePerSec <= 0L) return
        fractional += s.incomePerSec * seconds
        val whole = floor(fractional)
        if (whole >= 1.0) {
            fractional -= whole
            _gameState.value = s.copy(gpu = s.gpu + whole.toLong())
        }
    }

    fun buyUpgrade(upgradeId: String): Boolean {
        val s = _gameState.value
        val up = s.upgrades[upgradeId] ?: return false
        val cost = getUpgradeCost(upgradeId)
        if (s.gpu < cost) return false

        val newUpgrades = s.upgrades.toMutableMap()
        val newUp = up.copy(owned = up.owned + 1)
        newUpgrades[upgradeId] = newUp

        _gameState.value = s.copy(
            gpu = s.gpu - cost,
            upgrades = newUpgrades,
            multiplier = s.multiplier + newUp.effect * 0.05,
            workers = if (upgradeId == "worker_bot") s.workers + 1 else s.workers
        )
        return true
    }

    fun getUpgradeCost(upgradeId: String): Long {
        val up = _gameState.value.upgrades[upgradeId] ?: return 0L
        return (up.baseCost * up.costMultiplier.pow(up.owned.toDouble())).toLong()
    }

    /** Prestige: wipe run progress, keep a permanent +25% multiplier per reset. */
    fun prestige(): Boolean {
        val s = _gameState.value
        if (!s.canPrestige) return false
        fractional = 0.0
        _gameState.value = GameState(prestige = s.prestige + 1, totalTaps = s.totalTaps)
        return true
    }

    fun resetAll() {
        fractional = 0.0
        _gameState.value = GameState()
    }

    // ---- Persistence (plain SharedPreferences, no serialization deps) ----

    fun saveTo(prefs: SharedPreferences) {
        val s = _gameState.value
        prefs.edit()
            .putLong("gpu", s.gpu)
            .putLong("totalTaps", s.totalTaps)
            .putInt("level", s.level)
            .putInt("workers", s.workers)
            .putFloat("multiplier", s.multiplier.toFloat())
            .putInt("prestige", s.prestige)
            .putInt("u_gpu_farm", s.upgrades["gpu_farm"]?.owned ?: 0)
            .putInt("u_worker_bot", s.upgrades["worker_bot"]?.owned ?: 0)
            .putInt("u_processing_core", s.upgrades["processing_core"]?.owned ?: 0)
            .putInt("u_ai_booster", s.upgrades["ai_booster"]?.owned ?: 0)
            .putLong("lastSave", System.currentTimeMillis())
            .apply()
    }

    /** Returns offline GPU earned since last save (capped at 2h). */
    fun loadFrom(prefs: SharedPreferences): Long {
        if (!prefs.contains("gpu")) return 0L

        val base = GameState.defaultUpgrades()
        val upgrades = base.mapValues { (id, info) ->
            info.copy(owned = prefs.getInt("u_$id", 0))
        }

        var s = GameState(
            gpu = prefs.getLong("gpu", 0),
            totalTaps = prefs.getLong("totalTaps", 0),
            level = prefs.getInt("level", 1),
            workers = prefs.getInt("workers", 0),
            multiplier = prefs.getFloat("multiplier", 1f).toDouble(),
            prestige = prefs.getInt("prestige", 0),
            upgrades = upgrades
        )

        // Offline earnings
        val lastSave = prefs.getLong("lastSave", System.currentTimeMillis())
        val elapsedSec = ((System.currentTimeMillis() - lastSave) / 1000.0)
            .coerceIn(0.0, 2 * 60 * 60.0) // cap 2h
        val offlineGain = (s.incomePerSec * elapsedSec).toLong()
        if (offlineGain > 0L) s = s.copy(gpu = s.gpu + offlineGain)

        _gameState.value = s
        return offlineGain
    }
}
