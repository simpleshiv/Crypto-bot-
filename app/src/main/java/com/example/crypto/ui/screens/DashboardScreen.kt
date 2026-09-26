package com.example.crypto.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.data.remote.WsConnectionState
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.IndicatorState
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.model.Ticker
import com.example.crypto.domain.model.TradingMode
import com.example.crypto.domain.model.TradingPosition
import com.example.crypto.domain.strategy.IndicatorCalculator
import com.example.crypto.ui.TradingUiState
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoError
import com.example.ui.theme.CryptoPrimary
import com.example.ui.theme.CryptoPurple
import com.example.ui.theme.CryptoSecondary
import com.example.ui.theme.CryptoSurface
import com.example.ui.theme.CryptoSurfaceBright
import com.example.ui.theme.CryptoSurfaceVariant
import com.example.ui.theme.CryptoTextMuted
import com.example.ui.theme.CryptoTextPrimary
import com.example.ui.theme.CryptoTextSecondary
import com.example.ui.theme.CryptoWarning
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun DashboardScreen(
    uiState: TradingUiState,
    onToggleBotActive: () -> Unit,
    onToggleTradingMode: (TradingMode) -> Unit,
    onEmergencyKillSwitch: () -> Unit,
    onSimulateSignal: (SignalType) -> Unit,
    onTimeframeChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showKillSwitchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Real-Time BTC Price Hero Header with Animated Flash
        LiveBtcPriceHeroHeader(
            ticker = uiState.ticker,
            mode = uiState.config.tradingMode,
            wsState = uiState.connectionState,
            priceDirection = uiState.priceChangeDirection,
            leverage = uiState.config.leverage,
            onModeToggle = onToggleTradingMode
        )

        // 2. Real-Time Net PnL & Equity Card
        RealTimePnlCard(
            totalBalance = uiState.accountBalance.totalWalletBalance,
            unrealizedProfit = uiState.accountBalance.unrealizedProfit,
            marginBalance = uiState.accountBalance.marginBalance,
            winRate = uiState.winRatePercent,
            totalTrades = uiState.totalTrades,
            totalRealizedPnl = uiState.totalRealizedPnl,
            activePositions = uiState.activePositions,
            isPaper = uiState.config.tradingMode == TradingMode.PAPER
        )

        // 3. Real-Time Interactive Candlestick + EMA + SL/TP Chart directly on Dashboard
        DashboardRealTimeChartCard(
            ticker = uiState.ticker,
            candles = uiState.candles,
            indicators = uiState.indicatorState,
            activePositions = uiState.activePositions,
            selectedTimeframe = uiState.selectedTimeframe,
            onSelectTimeframe = onTimeframeChange
        )

        // 4. Active Position Live Tracking or Quick Simulator Trigger
        if (uiState.activePositions.isNotEmpty()) {
            ActivePositionLiveCard(
                position = uiState.activePositions.first(),
                currentPrice = uiState.ticker.lastPrice,
                onClosePosition = { showKillSwitchDialog = true }
            )
        } else {
            QuickSimulatorAndPositionEmptyCard(
                isActive = uiState.config.isActive,
                onSimulateLong = { onSimulateSignal(SignalType.LONG) },
                onSimulateShort = { onSimulateSignal(SignalType.SHORT) }
            )
        }

        // 5. Bot Execution Engine & Emergency Kill Switch
        BotExecutionCard(
            isActive = uiState.config.isActive,
            onToggleActive = onToggleBotActive,
            onTriggerKillSwitch = { showKillSwitchDialog = true }
        )

        // 6. Multi-Indicator Strategy Matrix (3 out of 4 Rule)
        IndicatorAlignmentCard(
            indicators = uiState.indicatorState,
            symbol = uiState.config.symbol,
            latestSignal = uiState.latestSignal
        )

        // 7. Live Execution Stream Logs
        SystemEventStreamCard(logs = uiState.botLogs)
    }

    // Emergency Kill Switch Dialog
    if (showKillSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showKillSwitchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CryptoError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EMERGENCY KILL SWITCH", color = CryptoError, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This high-priority action will IMMEDIATELY:\n" +
                            "• Cancel all open orders and conditional SL/TP orders\n" +
                            "• Execute an immediate MARKET order to flatten all active positions\n" +
                            "• Pause the bot execution loop\n\n" +
                            "Are you sure you want to execute emergency liquidation?",
                    color = CryptoTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showKillSwitchDialog = false
                        onEmergencyKillSwitch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoError),
                    modifier = Modifier.testTag("confirm_kill_switch_button")
                ) {
                    Text("CONFIRM LIQUIDATE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showKillSwitchDialog = false }) {
                    Text("Cancel", color = CryptoTextPrimary)
                }
            },
            containerColor = CryptoSurfaceVariant
        )
    }
}

/**
 * 1. Live BTC Price Hero Header with Animated Price Flash & Streaming Beacon
 */
@Composable
private fun LiveBtcPriceHeroHeader(
    ticker: Ticker,
    mode: TradingMode,
    wsState: WsConnectionState,
    priceDirection: Int,
    leverage: Int = 20,
    onModeToggle: (TradingMode) -> Unit
) {
    // Pulse animation for real-time streaming badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Dynamic price color flash on new tick
    var flashColor by remember { mutableStateOf(CryptoTextPrimary) }
    LaunchedEffect(ticker.lastPrice) {
        if (priceDirection > 0) {
            flashColor = CryptoSecondary
            delay(400)
            flashColor = CryptoTextPrimary
        } else if (priceDirection < 0) {
            flashColor = CryptoError
            delay(400)
            flashColor = CryptoTextPrimary
        }
    }

    val animatedPriceColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 300),
        label = "priceColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
        modifier = Modifier.fillMaxWidth().testTag("live_btc_hero_header")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Symbol, Live Beacon & Paper/Live Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${ticker.symbol} Perpetual",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = CryptoTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Pulsing Live Feed Beacon
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CryptoSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(CryptoSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REAL-TIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CryptoSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Multiplier Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CryptoWarning.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${leverage}X",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CryptoWarning
                        )
                    }
                }

                // Paper / Live Mode Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (mode == TradingMode.PAPER) "PAPER" else "LIVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (mode == TradingMode.PAPER) CryptoPrimary else CryptoWarning
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = mode == TradingMode.LIVE,
                        onCheckedChange = { isLive ->
                            onModeToggle(if (isLive) TradingMode.LIVE else TradingMode.PAPER)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CryptoWarning,
                            checkedTrackColor = CryptoWarning.copy(alpha = 0.3f),
                            uncheckedThumbColor = CryptoPrimary,
                            uncheckedTrackColor = CryptoPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("mode_toggle_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Live Price & 24h Change Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE BITCOIN PRICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${String.format("%,.2f", ticker.lastPrice)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = animatedPriceColor,
                        modifier = Modifier.testTag("live_btc_price_text")
                    )
                }

                // 24h Change Badge
                val isPositive = ticker.priceChangePercent >= 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPositive) CryptoSecondary.copy(alpha = 0.15f) else CryptoError.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) CryptoSecondary else CryptoError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", ticker.priceChangePercent)}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isPositive) CryptoSecondary else CryptoError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 24h Stats Row: High, Low, Volume
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CryptoSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("24h High", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.1f", ticker.highPrice)}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoTextPrimary
                    )
                }

                Column {
                    Text("24h Low", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.1f", ticker.lowPrice)}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("24h Vol (BTC)", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = String.format("%,.1f", ticker.volume),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * 2. Real-Time Net PnL & Equity Card
 */
@Composable
private fun RealTimePnlCard(
    totalBalance: Double,
    unrealizedProfit: Double,
    marginBalance: Double,
    winRate: Double,
    totalTrades: Int,
    totalRealizedPnl: Double,
    activePositions: List<TradingPosition>,
    isPaper: Boolean
) {
    val totalNetPnl = totalRealizedPnl + unrealizedProfit
    val isNetProfit = totalNetPnl >= 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (unrealizedProfit > 0) CryptoSecondary.copy(alpha = 0.4f)
            else if (unrealizedProfit < 0) CryptoError.copy(alpha = 0.4f)
            else CryptoBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("realtime_pnl_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME PNL & EQUITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CryptoTextMuted
                )

                // Live PnL Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (unrealizedProfit >= 0) CryptoSecondary.copy(alpha = 0.15f)
                            else CryptoError.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (activePositions.isNotEmpty()) "POSITION LIVE" else "READY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unrealizedProfit >= 0) CryptoSecondary else CryptoError
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main PnL Value Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Total Net PnL", fontSize = 11.sp, color = CryptoTextSecondary)
                    Text(
                        text = "${if (isNetProfit) "+" else ""}${String.format("%,.2f", totalNetPnl)} USDT",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isNetProfit) CryptoSecondary else CryptoError,
                        modifier = Modifier.testTag("net_pnl_value_text")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Live Unrealized PnL", fontSize = 11.sp, color = CryptoTextSecondary)
                    Text(
                        text = "${if (unrealizedProfit >= 0) "+" else ""}${String.format("%,.2f", unrealizedProfit)} USDT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (unrealizedProfit >= 0) CryptoSecondary else CryptoError,
                        modifier = Modifier.testTag("unrealized_pnl_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Quadrant Metric Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CryptoSurfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Wallet Balance", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.2f", totalBalance)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoTextPrimary
                    )
                }

                Column {
                    Text("Realized PnL", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "${if (totalRealizedPnl >= 0) "+" else ""}${String.format("%.2f", totalRealizedPnl)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (totalRealizedPnl >= 0) CryptoSecondary else CryptoError
                    )
                }

                Column {
                    Text("Win Rate", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "${String.format("%.1f", winRate)}%",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Trades", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$totalTrades",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * 3. Real-Time Interactive Candlestick + EMA + SL/TP Chart directly on Dashboard
 */
@Composable
private fun DashboardRealTimeChartCard(
    ticker: Ticker,
    candles: List<Candle>,
    indicators: IndicatorState,
    activePositions: List<TradingPosition>,
    selectedTimeframe: String,
    onSelectTimeframe: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
        modifier = Modifier.fillMaxWidth().height(320.dp).testTag("dashboard_chart_card")
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Chart Top Controls: Timeframe chips & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Chips (1m, 5m, 15m)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1m", "5m", "15m").forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CryptoPrimary else CryptoSurfaceVariant)
                                .clickable { onSelectTimeframe(tf) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else CryptoTextMuted
                            )
                        }
                    }
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(CryptoPrimary, CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("EMA 50", fontSize = 10.sp, color = CryptoPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(CryptoPurple, CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("EMA 200", fontSize = 10.sp, color = CryptoPurple)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Candlestick Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B12))
            ) {
                if (candles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading Live Bitcoin Candlesticks...", color = CryptoTextMuted, fontSize = 12.sp)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().testTag("dashboard_candlestick_canvas")) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val visibleCandles = candles.takeLast(40)
                        if (visibleCandles.isEmpty()) return@Canvas

                        var minPrice = visibleCandles.minOf { it.low }
                        var maxPrice = visibleCandles.maxOf { it.high }

                        // Include live ticker price in scale if valid
                        if (ticker.lastPrice > 0.0 && !ticker.lastPrice.isNaN() && !ticker.lastPrice.isInfinite()) {
                            minPrice = min(minPrice, ticker.lastPrice)
                            maxPrice = max(maxPrice, ticker.lastPrice)
                        }

                        // Include active SL/TP in price scale if open and valid
                        activePositions.firstOrNull()?.let { pos ->
                            if (pos.stopLoss > 0.0 && !pos.stopLoss.isNaN() && !pos.stopLoss.isInfinite()) {
                                minPrice = min(minPrice, pos.stopLoss)
                                maxPrice = max(maxPrice, pos.stopLoss)
                            }
                            if (pos.takeProfit > 0.0 && !pos.takeProfit.isNaN() && !pos.takeProfit.isInfinite()) {
                                minPrice = min(minPrice, pos.takeProfit)
                                maxPrice = max(maxPrice, pos.takeProfit)
                            }
                        }

                        val priceRange = max(1.0, maxPrice - minPrice)
                        val paddingRatio = 0.08
                        val paddedMin = minPrice - priceRange * paddingRatio
                        val paddedMax = maxPrice + priceRange * paddingRatio
                        val paddedRange = max(1.0, paddedMax - paddedMin)

                        fun priceToY(price: Double): Float {
                            if (price.isNaN() || price.isInfinite()) return canvasHeight / 2f
                            val normalized = ((price - paddedMin) / paddedRange).coerceIn(-0.5, 1.5)
                            val y = (canvasHeight * (1.0 - normalized)).toFloat()
                            return if (y.isNaN() || y.isInfinite()) canvasHeight / 2f else y
                        }

                        // Draw Grid Lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = (canvasHeight / gridCount) * i
                            drawLine(
                                color = CryptoBorder.copy(alpha = 0.4f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        val candleCount = visibleCandles.size
                        val slotWidth = canvasWidth / candleCount
                        val candleWidth = max(3f, slotWidth * 0.68f)

                        // Draw Candlesticks
                        for (i in visibleCandles.indices) {
                            val c = visibleCandles[i]
                            val x = i * slotWidth + slotWidth / 2f
                            val isBullish = c.close >= c.open
                            val candleColor = if (isBullish) CryptoSecondary else CryptoError

                            val highY = priceToY(c.high)
                            val lowY = priceToY(c.low)
                            val openY = priceToY(c.open)
                            val closeY = priceToY(c.close)

                            // Wick
                            drawLine(
                                color = candleColor,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = 1.5f
                            )

                            // Body
                            val topY = min(openY, closeY)
                            val bottomY = max(openY, closeY)
                            val bodyHeight = max(2f, bottomY - topY)

                            drawRect(
                                color = candleColor,
                                topLeft = Offset(x - candleWidth / 2f, topY),
                                size = Size(candleWidth, bodyHeight)
                            )
                        }

                        // Overlay 50 EMA & 200 EMA curves safely
                        val closePrices = candles.map { it.close }
                        if (closePrices.size >= 10) {
                            val fastEma = IndicatorCalculator.calculateEMA(closePrices, 50.coerceAtMost(max(10, closePrices.size / 2)))
                            val slowEma = IndicatorCalculator.calculateEMA(closePrices, 100.coerceAtMost(max(20, closePrices.size - 2)))

                            fun drawEmaCurve(emaList: List<Double>, color: Color) {
                                if (emaList.size < candleCount) return
                                val visibleEma = emaList.takeLast(candleCount)
                                val path = Path()
                                var hasPoint = false

                                for (i in visibleEma.indices) {
                                    val valEma = visibleEma[i]
                                    if (valEma.isNaN() || valEma.isInfinite() || valEma <= 0.0) continue
                                    val x = i * slotWidth + slotWidth / 2f
                                    val y = priceToY(valEma)
                                    if (y.isNaN() || y.isInfinite()) continue
                                    if (!hasPoint) {
                                        path.moveTo(x, y)
                                        hasPoint = true
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                if (hasPoint) {
                                    drawPath(path = path, color = color, style = Stroke(width = 2f))
                                }
                            }

                            drawEmaCurve(fastEma, CryptoPrimary)
                            drawEmaCurve(slowEma, CryptoPurple)
                        }

                        // LIVE BEACON: Real-Time Current Price Line
                        if (ticker.lastPrice > 0.0 && !ticker.lastPrice.isNaN()) {
                            val currentY = priceToY(ticker.lastPrice)
                            drawLine(
                                color = CryptoPrimary,
                                start = Offset(0f, currentY),
                                end = Offset(canvasWidth - 60f, currentY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Live price tag on right axis
                            drawRect(
                                color = CryptoPrimary,
                                topLeft = Offset(canvasWidth - 62f, currentY - 10f),
                                size = Size(62f, 20f)
                            )
                        }

                        // Active Position SL / TP overlays
                        activePositions.firstOrNull()?.let { pos ->
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                            // Stop Loss (Red)
                            if (pos.stopLoss > 0.0 && !pos.stopLoss.isNaN() && !pos.stopLoss.isInfinite()) {
                                val slY = priceToY(pos.stopLoss)
                                if (!slY.isNaN() && !slY.isInfinite()) {
                                    drawLine(
                                        color = CryptoError,
                                        start = Offset(0f, slY),
                                        end = Offset(canvasWidth, slY),
                                        strokeWidth = 2f,
                                        pathEffect = dashedEffect
                                    )
                                }
                            }

                            // Take Profit (Green)
                            if (pos.takeProfit > 0.0 && !pos.takeProfit.isNaN() && !pos.takeProfit.isInfinite()) {
                                val tpY = priceToY(pos.takeProfit)
                                if (!tpY.isNaN() && !tpY.isInfinite()) {
                                    drawLine(
                                        color = CryptoSecondary,
                                        start = Offset(0f, tpY),
                                        end = Offset(canvasWidth, tpY),
                                        strokeWidth = 2f,
                                        pathEffect = dashedEffect
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Active Position Live Card
 */
@Composable
private fun ActivePositionLiveCard(
    position: TradingPosition,
    currentPrice: Double,
    onClosePosition: () -> Unit
) {
    val isLong = position.side == SignalType.LONG
    val pnl = position.unrealizedPnl
    val pnlPercent = position.unrealizedPnlPercent

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (pnl >= 0) CryptoSecondary else CryptoError
        ),
        modifier = Modifier.fillMaxWidth().testTag("active_position_live_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLong) CryptoSecondary else CryptoError)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${position.side.name} ${position.leverage}X",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = position.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CryptoTextPrimary
                    )
                }

                // Live PnL
                Text(
                    text = "${if (pnl >= 0) "+" else ""}${String.format("%.2f", pnl)} USDT (${String.format("%.2f", pnlPercent)}%)",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = if (pnl >= 0) CryptoSecondary else CryptoError
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Price", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.2f", position.entryPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoTextPrimary
                    )
                }

                Column {
                    Text("Live Mark Price", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.2f", currentPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Size", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "${position.quantity} BTC",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CryptoTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic SL / TP target bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CryptoSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (position.isSlTrailed) "Trailed SL: " else "Dynamic SL: ",
                            fontSize = 11.sp,
                            color = CryptoTextMuted
                        )
                        Text(
                            text = "$${String.format("%,.2f", position.stopLoss)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (position.isProfitLocked) CryptoSecondary else CryptoError
                        )
                        if (position.isSlTrailed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (position.isProfitLocked) "🔒" else "📈", fontSize = 10.sp)
                        }
                    }
                    val peakPrice = if (isLong) position.highestPrice else position.lowestPrice
                    if (peakPrice > 0.0 && peakPrice != position.entryPrice) {
                        Text(
                            text = "${if (isLong) "Peak" else "Trough"}: $${String.format("%,.1f", peakPrice)}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CryptoTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Dynamic TP: $${String.format("%,.2f", position.takeProfit)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CryptoSecondary
                    )
                }
            }

            // Trailing Stop Loss Active Notification Banner
            if (position.isTrailingSlActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (position.isProfitLocked) CryptoSecondary.copy(alpha = 0.15f) else CryptoPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (position.isProfitLocked) CryptoSecondary else CryptoPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (position.isProfitLocked) "PROFIT LOCKED: SL Above Entry" else "AUTO-TRAILING SL ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (position.isProfitLocked) CryptoSecondary else CryptoPrimary
                        )
                    }

                    Text(
                        text = if (position.isSlTrailed) "SL Ratcheted Up" else "Trailing Peak",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onClosePosition,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CryptoError),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("close_position_button")
            ) {
                Text("Market Close Position", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 4b. Empty State with Quick Simulation Buttons
 */
@Composable
private fun QuickSimulatorAndPositionEmptyCard(
    isActive: Boolean,
    onSimulateLong: () -> Unit,
    onSimulateShort: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_simulator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isActive) "AUTOMATED ENGINE SCANNING FOR SIGNALS" else "NO OPEN POSITIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CryptoTextMuted
                )
                Text("QUICK TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CryptoPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap below to test order execution & watch real-time PnL calculate live:",
                fontSize = 12.sp,
                color = CryptoTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSimulateLong,
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("simulate_long_button")
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test LONG", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSimulateShort,
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("simulate_short_button")
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test SHORT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 5. Bot Controls & Kill Switch
 */
@Composable
private fun BotExecutionCard(
    isActive: Boolean,
    onToggleActive: () -> Unit,
    onTriggerKillSwitch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("bot_controls_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "QUANTITATIVE BOT CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CryptoTextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) CryptoSurfaceBright else CryptoSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("bot_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isActive) CryptoWarning else Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "PAUSE BOT" else "START BOT",
                        color = if (isActive) CryptoTextPrimary else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onTriggerKillSwitch,
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("kill_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KILL SWITCH",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * 6. Strategy 3/4 Alignment Matrix
 */
@Composable
private fun IndicatorAlignmentCard(
    indicators: IndicatorState,
    symbol: String,
    latestSignal: com.example.crypto.domain.model.TradeSignal?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STRATEGY ENGINE (3/4 RULE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CryptoTextMuted
                )
                val isAligned = indicators.longIndicatorsMet >= 3 || indicators.shortIndicatorsMet >= 3
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAligned) CryptoSecondary.copy(alpha = 0.2f) else CryptoSurfaceBright)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (indicators.longIndicatorsMet >= 3)
                            "LONG SIGNAL READY (${indicators.longIndicatorsMet}/4)"
                        else if (indicators.shortIndicatorsMet >= 3)
                            "SHORT SIGNAL READY (${indicators.shortIndicatorsMet}/4)"
                        else "SCANNING (${maxOf(indicators.longIndicatorsMet, indicators.shortIndicatorsMet)}/4)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) CryptoSecondary else CryptoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            IndicatorRow(
                name = "1. Trend: 50 / 200 EMA Cross",
                value = "Fast: ${String.format("%.0f", indicators.emaFast)} | Slow: ${String.format("%.0f", indicators.emaSlow)}",
                status = if (indicators.isGoldenCross) "Golden Cross (Long)" else if (indicators.isDeathCross) "Death Cross (Short)" else "Neutral",
                isBullish = indicators.isGoldenCross,
                isBearish = indicators.isDeathCross
            )

            Spacer(modifier = Modifier.height(6.dp))

            IndicatorRow(
                name = "2. Strength: ADX(14) > 25",
                value = String.format("%.1f", indicators.adx),
                status = if (indicators.isAdxStrong) "Trending Market (>25)" else "Choppy (<25)",
                isBullish = indicators.isAdxStrong,
                isBearish = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            IndicatorRow(
                name = "3. Momentum: RSI(14)",
                value = String.format("%.1f", indicators.rsi),
                status = if (indicators.isRsiOversold) "Oversold (<30) Long" else if (indicators.isRsiOverbought) "Overbought (>70) Short" else "Neutral (30-70)",
                isBullish = indicators.isRsiOversold,
                isBearish = indicators.isRsiOverbought
            )

            Spacer(modifier = Modifier.height(6.dp))

            IndicatorRow(
                name = "4. Confirmation: MACD (12,26,9)",
                value = "Hist: ${String.format("%.2f", indicators.macdHist)}",
                status = if (indicators.isMacdBullish) "Bullish Cross" else if (indicators.isMacdBearish) "Bearish Cross" else "Neutral",
                isBullish = indicators.isMacdBullish,
                isBearish = indicators.isMacdBearish
            )
        }
    }
}

@Composable
private fun IndicatorRow(
    name: String,
    value: String,
    status: String,
    isBullish: Boolean,
    isBearish: Boolean
) {
    val statusColor = if (isBullish) CryptoSecondary else if (isBearish) CryptoError else CryptoTextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CryptoSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CryptoTextPrimary)
            Text(text = value, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CryptoTextMuted)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

/**
 * 7. Live Execution Stream Logs
 */
@Composable
private fun SystemEventStreamCard(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LIVE EXECUTION STREAM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CryptoTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF070A0F))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Awaiting trade triggers...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoTextMuted
                    )
                } else {
                    logs.take(5).forEach { logLine ->
                        Text(
                            text = logLine,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                logLine.contains("FILLED") || logLine.contains("CLOSED_TP") -> CryptoSecondary
                                logLine.contains("KILL SWITCH") || logLine.contains("CLOSED_SL") || logLine.contains("ERROR") -> CryptoError
                                logLine.contains("ATTACHED") || logLine.contains("CONNECTED") -> CryptoPrimary
                                else -> CryptoTextSecondary
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
