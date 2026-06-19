package com.secondream.aiidler

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

data class Generator(
    val id: String,
    val name: String,
    val icon: String,
    val baseCost: Double,
    val baseRate: Double,
    val costMult: Double = 1.15,
    val count: Int = 0
) {
    fun cost(): Double = baseCost * costMult.pow(count.toDouble())
    fun rate(globalMult: Double): Double = baseRate * count * globalMult
}

data class CompanyStage(val name: String, val threshold: Double)

data class GameState(
    val cash: Double = 0.0,
    val lifetimeEarned: Double = 0.0,
    val shares: Int = 0,
    val totalTaps: Long = 0,
    val generators: List<Generator> = defaultGenerators()
) {
    val globalMult: Double get() = 1.0 + shares * 0.10
    val incomePerSec: Double get() = generators.sumOf { it.rate(globalMult) }
    val valuation: Double get() = lifetimeEarned

    val stageIndex: Int
        get() {
            var idx = 0
            for (i in STAGES.indices) if (valuation >= STAGES[i].threshold) idx = i
            return idx
        }
    val stage: CompanyStage get() = STAGES[stageIndex]
    val nextStage: CompanyStage? get() = STAGES.getOrNull(stageIndex + 1)
    val canIpo: Boolean get() = valuation >= IPO_THRESHOLD

    fun tapValue(): Double = (1.0 + 0.5 * generators.sumOf { it.count }) * globalMult

    companion object {
        const val IPO_THRESHOLD = 1_000_000.0

        val STAGES = listOf(
            CompanyStage("Cameretta", 0.0),
            CompanyStage("Indie Dev", 1_000.0),
            CompanyStage("Startup", 50_000.0),
            CompanyStage("Startup finanziata", 1_000_000.0),
            CompanyStage("Scale-up", 50_000_000.0),
            CompanyStage("Unicorno", 1_000_000_000.0),
            CompanyStage("Big Lab", 100_000_000_000.0),
            CompanyStage("Rivale di OpenAI", 10_000_000_000_000.0)
        )

        fun defaultGenerators(): List<Generator> = listOf(
            Generator("phone", "Smartphone", "📱", 15.0, 0.1),
            Generator("laptop", "Laptop", "💻", 120.0, 1.0),
            Generator("pc", "Gaming PC", "🖥️", 1_300.0, 8.0),
            Generator("gpu", "GPU Rig", "🎮", 14_000.0, 47.0),
            Generator("rack", "Server Rack", "🗄️", 200_000.0, 260.0),
            Generator("datacenter", "Data Center", "🏢", 3_300_000.0, 1_400.0),
            Generator("cluster", "AI Cluster", "⚡", 55_000_000.0, 7_800.0),
            Generator("campus", "Hyperscale Campus", "🌐", 1_000_000_000.0, 44_000.0)
        )
    }
}

class GameLogic {
    private val _state = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _state

    fun tapWork(): Double {
        val s = _state.value
        val gain = s.tapValue()
        _state.value = s.copy(
            cash = s.cash + gain,
            lifetimeEarned = s.lifetimeEarned + gain,
            totalTaps = s.totalTaps + 1
        )
        return gain
    }

    fun accrue(seconds: Double) {
        val s = _state.value
        val inc = s.incomePerSec
        if (inc <= 0.0) return
        val gain = inc * seconds
        _state.value = s.copy(cash = s.cash + gain, lifetimeEarned = s.lifetimeEarned + gain)
    }

    fun buy(id: String): Boolean {
        val s = _state.value
        val idx = s.generators.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val g = s.generators[idx]
        val cost = g.cost()
        if (s.cash < cost) return false
        val gens = s.generators.toMutableList()
        gens[idx] = g.copy(count = g.count + 1)
        _state.value = s.copy(cash = s.cash - cost, generators = gens)
        return true
    }

    fun ipoShares(): Int {
        val v = _state.value.valuation
        return floor(sqrt(v / IPO_BASE)).toInt().coerceAtLeast(0)
    }

    fun ipo(): Boolean {
        val s = _state.value
        if (!s.canIpo) return false
        val gained = ipoShares().coerceAtLeast(1)
        _state.value = GameState(shares = s.shares + gained, totalTaps = s.totalTaps)
        return true
    }

    fun resetAll() {
        _state.value = GameState()
    }

    // ---- Persistence (SharedPreferences; doubles stored as strings) ----

    fun saveTo(prefs: SharedPreferences) {
        val s = _state.value
        val e = prefs.edit()
        e.putString("cash", s.cash.toString())
        e.putString("lifetime", s.lifetimeEarned.toString())
        e.putInt("shares", s.shares)
        e.putLong("totalTaps", s.totalTaps)
        s.generators.forEach { e.putInt("g_${it.id}", it.count) }
        e.putLong("lastSave", System.currentTimeMillis())
        e.apply()
    }

    /** Returns offline cash earned since last save (capped at 2h). */
    fun loadFrom(prefs: SharedPreferences): Double {
        if (!prefs.contains("cash")) return 0.0
        val gens = GameState.defaultGenerators().map { it.copy(count = prefs.getInt("g_${it.id}", 0)) }
        var s = GameState(
            cash = (prefs.getString("cash", "0") ?: "0").toDoubleOrNull() ?: 0.0,
            lifetimeEarned = (prefs.getString("lifetime", "0") ?: "0").toDoubleOrNull() ?: 0.0,
            shares = prefs.getInt("shares", 0),
            totalTaps = prefs.getLong("totalTaps", 0),
            generators = gens
        )
        val lastSave = prefs.getLong("lastSave", System.currentTimeMillis())
        val elapsed = ((System.currentTimeMillis() - lastSave) / 1000.0).coerceIn(0.0, 2 * 60 * 60.0)
        val offline = s.incomePerSec * elapsed
        if (offline > 0.0) s = s.copy(cash = s.cash + offline, lifetimeEarned = s.lifetimeEarned + offline)
        _state.value = s
        return offline
    }

    companion object {
        private const val IPO_BASE = 1_000_000.0
    }
}

fun formatNumber(value: Double): String {
    if (value < 1000.0) return value.toLong().toString()
    val units = listOf("K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")
    var v = value
    var i = -1
    while (v >= 1000.0 && i < units.size - 1) {
        v /= 1000.0
        i++
    }
    return "%.2f%s".format(v, units[i])
}
