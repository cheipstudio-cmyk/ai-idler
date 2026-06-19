package com.secondream.aiidler

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val codeTokens: Long = 0,
    val totalTaps: Long = 0,
    val level: Int = 1,
    val workers: Int = 0,
    val multiplier: Double = 1.0,
    val gpuPerSecond: Double = 0.0,
    val upgrades: Map<String, UpgradeInfo> = mapOf(
        "gpu_farm" to UpgradeInfo("gpu_farm", "GPU Farm", 10),
        "worker_bot" to UpgradeInfo("worker_bot", "Worker Bot", 50, effect = 1.0),
        "processing_core" to UpgradeInfo("processing_core", "Processing Core", 150, effect = 1.5),
        "ai_booster" to UpgradeInfo("ai_booster", "AI Booster", 500, effect = 2.0)
    )
)

class GameLogic {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    fun tap(): Pair<Long, Boolean> {
        val currentState = _gameState.value
        val farmOwned = currentState.upgrades["gpu_farm"]?.owned ?: 0
        val baseGain = 1L + (currentState.multiplier * farmOwned).toLong()
        val totalGain = baseGain * 100 // 1 GPU = 100 units for finer progression
        
        val newGpu = currentState.gpu + totalGain
        val breakthroughTriggered = newGpu > (currentState.level * 100000L)
        
        _gameState.value = currentState.copy(
            gpu = newGpu,
            totalTaps = currentState.totalTaps + 1,
            level = if (breakthroughTriggered) currentState.level + 1 else currentState.level,
            multiplier = currentState.multiplier * (if (breakthroughTriggered) 1.1 else 1.0)
        )
        
        return Pair(totalGain, breakthroughTriggered)
    }

    fun buyUpgrade(upgradeId: String): Boolean {
        val currentState = _gameState.value
        val upgrade = currentState.upgrades[upgradeId] ?: return false
        val cost = (upgrade.baseCost * upgrade.costMultiplier.pow(upgrade.owned.toDouble())).toLong()
        
        if (currentState.gpu < cost) return false
        
        val newUpgrades = currentState.upgrades.toMutableMap()
        val newUpgrade = upgrade.copy(owned = upgrade.owned + 1)
        newUpgrades[upgradeId] = newUpgrade
        
        val newMultiplier = currentState.multiplier + (newUpgrade.effect * 0.05)
        val newWorkers = if (upgradeId == "worker_bot") currentState.workers + 1 else currentState.workers
        
        _gameState.value = currentState.copy(
            gpu = currentState.gpu - cost,
            upgrades = newUpgrades,
            multiplier = newMultiplier,
            workers = newWorkers
        )
        
        return true
    }

    fun offlineProgress(secondsAway: Long): Long {
        val currentState = _gameState.value
        val gpuPerSecond = 10L * currentState.workers // Each worker generates 10 GPU/sec offline
        return gpuPerSecond * secondsAway
    }

    fun applyOfflineProgress(gpuGain: Long) {
        val currentState = _gameState.value
        _gameState.value = currentState.copy(
            gpu = currentState.gpu + gpuGain
        )
    }

    fun getUpgradeCost(upgradeId: String): Long {
        val upgrade = gameState.value.upgrades[upgradeId] ?: return 0L
        return (upgrade.baseCost * upgrade.costMultiplier.pow(upgrade.owned.toDouble())).toLong()
    }
}
