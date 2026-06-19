package com.secondream.aiidler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.ln

private val BgTop = Color(0xFF1C3F77)
private val BgBottom = Color(0xFF0B1C39)
private val CardBg = Color(0xFFEFF4FC)
private val CardText = Color(0xFF12203A)
private val CardSub = Color(0xFF5A6B86)
private val Buy = Color(0xFF2FB14C)
private val BuyOff = Color(0xFF8A93A3)
private val Gold = Color(0xFFF2C14E)
private val NavBg = Color(0xFF0A1730)
private val NavActive = Color(0xFF6D3BD1)
private val Pill = Color(0x4D000000)
private val CardBg2 = Color(0xFF14264A)

private enum class Tab { HARDWARE, COMPANY }

@Composable
fun GameScreen(
    gameLogic: GameLogic,
    audioManager: AudioManager,
    hapticManager: HapticManager,
    offlineGain: Double,
    startOnboarding: Boolean,
    onSave: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit
) {
    val gameState by gameLogic.gameState.collectAsState()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(Tab.HARDWARE) }
    var showOnboarding by remember { mutableStateOf(startOnboarding) }
    var showSettings by remember { mutableStateOf(false) }
    var showConfirmReset by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(offlineGain > 0.0) }
    var soundOn by remember { mutableStateOf(audioManager.enabled) }
    var hapticsOn by remember { mutableStateOf(hapticManager.enabled) }

    // Idle income loop
    LaunchedEffect(Unit) {
        var last = System.currentTimeMillis()
        while (true) {
            kotlinx.coroutines.delay(200)
            val now = System.currentTimeMillis()
            gameLogic.accrue((now - last) / 1000.0)
            last = now
        }
    }
    // Autosave
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000)
            onSave()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                cash = gameState.cash,
                incomePerSec = gameState.incomePerSec,
                stageName = gameState.stage.name,
                onMenu = { showSettings = true }
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (tab) {
                    Tab.HARDWARE -> HardwareTab(gameState, gameLogic, audioManager)
                    Tab.COMPANY -> CompanyTab(gameState, gameLogic, audioManager, hapticManager, scope)
                }
            }

            BottomNav(tab = tab, onTab = { tab = it })
        }

        if (showOffline) OfflineOverlay(gain = offlineGain) { showOffline = false }

        if (showOnboarding) {
            OnboardingOverlay {
                showOnboarding = false
                onFinishOnboarding()
            }
        }

        if (showSettings) {
            SettingsOverlay(
                soundOn = soundOn,
                hapticsOn = hapticsOn,
                canIpo = gameState.canIpo,
                ipoShares = gameLogic.ipoShares(),
                shares = gameState.shares,
                onSound = { soundOn = it; audioManager.enabled = it; onToggleSound(it) },
                onHaptics = { hapticsOn = it; hapticManager.enabled = it; onToggleHaptics(it) },
                onIpo = {
                    if (gameLogic.ipo()) {
                        audioManager.playBreakthroughSound()
                        onSave()
                        showSettings = false
                    }
                },
                onResetRequest = { showConfirmReset = true },
                onClose = { showSettings = false }
            )
        }

        if (showConfirmReset) {
            ConfirmResetOverlay(
                onConfirm = {
                    gameLogic.resetAll()
                    onSave()
                    showConfirmReset = false
                    showSettings = false
                },
                onCancel = { showConfirmReset = false }
            )
        }
    }
}

@Composable
private fun TopBar(cash: Double, incomePerSec: Double, stageName: String, onMenu: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Pill)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("\uD83D\uDCB5  \$${formatNumber(cash)}", color = Color.White, fontSize = 22.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Pill)
                    .clickable { onMenu() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("MENU", color = Color.White, fontSize = 13.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Pill)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("\u26A1 \$${formatNumber(incomePerSec)}/sec", color = Gold, fontSize = 14.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Pill)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("\uD83C\uDFE2 $stageName", color = Color(0xFFB8C9E6), fontSize = 14.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HardwareTab(gameState: GameState, gameLogic: GameLogic, audioManager: AudioManager) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        gameState.generators.forEach { g ->
            GeneratorCard(
                generator = g,
                rate = g.rate(gameState.globalMult),
                cost = g.cost(),
                canAfford = gameState.cash >= g.cost(),
                onBuy = { if (gameLogic.buy(g.id)) audioManager.playUpgradeSound() }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GeneratorCard(
    generator: Generator,
    rate: Double,
    cost: Double,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDCE6F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(generator.icon, fontSize = 28.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(generator.name, color = CardText, fontSize = 17.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A4DA0))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("×${generator.count}", color = Color.White, fontSize = 12.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    }
                }
                Text("\$${formatNumber(rate)}/sec", color = CardSub, fontSize = 13.sp, fontFamily = Rajdhani, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAfford) Buy else BuyOff)
                    .clickable(enabled = canAfford) { onBuy() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("\$${formatNumber(cost)}", color = Color.White, fontSize = 15.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CompanyTab(
    gameState: GameState,
    gameLogic: GameLogic,
    audioManager: AudioManager,
    hapticManager: HapticManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val btnScale = remember { Animatable(1f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(gameState.stage.name.uppercase(), color = Gold, fontSize = 30.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("valutazione \$${formatNumber(gameState.valuation)}", color = Color(0xFFB8C9E6), fontSize = 15.sp, fontFamily = Rajdhani)
        Spacer(Modifier.height(16.dp))

        // Progress to next stage (log scale)
        val next = gameState.nextStage
        if (next != null) {
            val cur = gameState.stage.threshold.coerceAtLeast(1.0)
            val v = gameState.valuation.coerceAtLeast(1.0)
            val frac = ((ln(v) - ln(cur)) / (ln(next.threshold) - ln(cur))).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(Gold)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("prossimo: ${next.name}  (\$${formatNumber(next.threshold)})", color = Color(0xFF8FA6C8), fontSize = 12.sp, fontFamily = Rajdhani)
        } else {
            Text("Hai raggiunto il vertice del settore.", color = Gold, fontSize = 14.sp, fontFamily = Rajdhani)
        }

        if (gameState.shares > 0) {
            Spacer(Modifier.height(10.dp))
            Text("AZIONI IPO ×${gameState.shares}  (+${gameState.shares * 10}%)", color = Color(0xFF2FB14C), fontSize = 14.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(40.dp))

        // Manual train button
        Box(
            modifier = Modifier
                .scale(btnScale.value)
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF4A9EFF), Color(0xFF2A5DC0))))
                .clickable {
                    val gain = gameLogic.tapWork()
                    audioManager.playTapSound()
                    hapticManager.tap()
                    scope.launch {
                        btnScale.snapTo(0.9f)
                        btnScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83E\uDDE0", fontSize = 48.sp)
                Spacer(Modifier.height(4.dp))
                Text("ALLENA", color = Color.White, fontSize = 18.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("+\$${formatNumber(gameState.tapValue())} per tap", color = Color(0xFFB8C9E6), fontSize = 14.sp, fontFamily = Rajdhani)
    }
}

@Composable
private fun BottomNav(tab: Tab, onTab: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBg)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        NavItem("\uD83D\uDDA5\uFE0F", "HARDWARE", tab == Tab.HARDWARE) { onTab(Tab.HARDWARE) }
        NavItem("\uD83C\uDFE2", "AZIENDA", tab == Tab.COMPANY) { onTab(Tab.COMPANY) }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) NavActive else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = if (active) Color.White else Color(0xFF8FA6C8), fontSize = 12.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, color: Color = Color(0xFF4A9EFF), onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) color else Color(0xFF2A2A30))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else Color(0xFF6A6A70), fontSize = 16.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OnboardingOverlay(onStart: () -> Unit) {
    Scrim {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg2)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AI IDLER", color = Gold, fontSize = 28.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Parti dalla cameretta con uno smartphone.", color = Color.White, fontSize = 15.sp, fontFamily = Rajdhani, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Allena modelli, guadagna, compra hardware: laptop, GPU, server, data center.", color = Color.White, fontSize = 15.sp, fontFamily = Rajdhani, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Fai crescere la tua compagnia fino a sfidare OpenAI e Anthropic. I tuoi impianti guadagnano anche da spento, fino a 2 ore.", color = Color(0xFFB8C9E6), fontSize = 14.sp, fontFamily = Rajdhani, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("INIZIA", color = Gold, onClick = onStart)
        }
    }
}

@Composable
private fun OfflineOverlay(gain: Double, onCollect: () -> Unit) {
    Scrim {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg2)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BENTORNATO", color = Color(0xFF2FB14C), fontSize = 22.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("Mentre eri via i tuoi impianti hanno prodotto", color = Color(0xFFB8C9E6), fontSize = 14.sp, fontFamily = Rajdhani, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("+\$${formatNumber(gain)}", color = Color(0xFF4A9EFF), fontSize = 28.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("INCASSA", color = Color(0xFF2FB14C), onClick = onCollect)
        }
    }
}

@Composable
private fun SettingsOverlay(
    soundOn: Boolean,
    hapticsOn: Boolean,
    canIpo: Boolean,
    ipoShares: Int,
    shares: Int,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onIpo: () -> Unit,
    onResetRequest: () -> Unit,
    onClose: () -> Unit
) {
    Scrim {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg2)
                .padding(24.dp)
        ) {
            Text("OPZIONI", color = Gold, fontSize = 22.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            ToggleRow("Suono", soundOn, onSound)
            Spacer(Modifier.height(10.dp))
            ToggleRow("Vibrazione", hapticsOn, onHaptics)
            Spacer(Modifier.height(20.dp))
            Text("IPO", color = Color(0xFFB8C9E6), fontSize = 12.sp, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (canIpo) "Quota la compagnia: +${ipoShares * 10}% permanente. Azzera la run." else "Serve \$1M di valutazione per quotarti.",
                color = Color(0xFF8FA6C8), fontSize = 12.sp, fontFamily = Rajdhani
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                if (canIpo) "IPO (+${ipoShares * 10}%)" else "IPO BLOCCATA",
                enabled = canIpo,
                color = Color(0xFF2FB14C),
                onClick = onIpo
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton("AZZERA PROGRESSI", color = Color(0xFF8A3030), onClick = onResetRequest)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("CHIUDI", color = Color(0xFF33333A), onClick = onClose)
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontFamily = Rajdhani)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun ConfirmResetOverlay(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Scrim {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg2)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SICURO?", color = Color(0xFFE06060), fontSize = 22.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Cancella tutto, IPO incluse. Non si torna indietro.", color = Color(0xFFB8C9E6), fontSize = 14.sp, fontFamily = Rajdhani, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PrimaryButton("SÌ, AZZERA", color = Color(0xFF8A3030), onClick = onConfirm)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("ANNULLA", color = Color(0xFF33333A), onClick = onCancel)
        }
    }
}
