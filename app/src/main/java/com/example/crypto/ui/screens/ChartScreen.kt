package com.example.crypto.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.crypto.domain.model.AccountBalance
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.IndicatorState
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.model.Ticker
import com.example.crypto.domain.model.TradingPosition
import com.example.crypto.domain.strategy.IndicatorCalculator
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoError
import com.example.ui.theme.CryptoPrimary
import com.example.ui.theme.CryptoPurple
import com.example.ui.theme.CryptoSecondary
import com.example.ui.theme.CryptoSurface
import com.example.ui.theme.CryptoSurfaceVariant
import com.example.ui.theme.CryptoTextMuted
import com.example.ui.theme.CryptoTextPrimary
import com.example.ui.theme.CryptoTextSecondary
import com.example.ui.theme.CryptoWarning
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChartScreen(
    ticker: Ticker,
    candles: List<Candle>,
    indicators: IndicatorState,
    activePositions: List<TradingPosition>,
    accountBalance: AccountBalance = AccountBalance(),
    totalRealizedPnl: Double = 0.0,
    priceChangeDirection: Int = 0,
    onSimulateLong: () -> Unit = {},
    onSimulateShort: () -> Unit = {},
    onClosePosition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var subIndicatorMode by remember { mutableStateOf("RSI") } // "RSI" or "MACD"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Live BTC Real-Time Ticker Header with Tick Flash
        ChartTickerHeader(ticker = ticker, priceDirection = priceChangeDirection)

        // 2. Real-Time Position PnL & Equity Card
        ChartRealTimePnlCard(
            accountBalance = accountBalance,
            totalRealizedPnl = totalRealizedPnl,
            activePositions = activePositions,
            currentPrice = ticker.lastPrice,
            onSimulateLong = onSimulateLong,
            onSimulateShort = onSimulateShort,
            onClosePosition = onClosePosition
        )

        // 3. Main Candlestick Chart with Canvas
        CandlestickChartCard(
            ticker = ticker,
            candles = candles,
            indicators = indicators,
            activePositions = activePositions,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
        )

        // 4. Sub-chart Toggle & View (RSI or MACD)
        SubChartCard(
            candles = candles,
            indicators = indicators,
            selectedMode = subIndicatorMode,
            onSelectMode = { subIndicatorMode = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        )
    }
}

@Composable
private fun ChartTickerHeader(
    ticker: Ticker,
    priceDirection: Int
) {
    // Pulse beacon animation
    val infiniteTransition = rememberInfiniteTransition(label = "chartPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chartPulseAlpha"
    )

    // Flash price color on tick
    var flashColor by remember { mutableStateOf(CryptoTextPrimary) }
    LaunchedEffect(ticker.lastPrice) {
        if (priceDirection > 0) {
            flashColor = CryptoSecondary
            delay(350)
            flashColor = CryptoTextPrimary
        } else if (priceDirection < 0) {
            flashColor = CryptoError
            delay(350)
            flashColor = CryptoTextPrimary
        }
    }

    val animatedPriceColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 250),
        label = "chartPriceColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
        modifier = Modifier.fillMaxWidth().testTag("chart_ticker_header")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${ticker.symbol} Perp",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = CryptoTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Pulsing LIVE dot
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CryptoSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(CryptoSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CryptoSecondary
                        )
                    }
                }

                // Price and 24h change badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%,.2f", ticker.lastPrice)}",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        color = animatedPriceColor,
                        modifier = Modifier.testTag("chart_live_btc_price")
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val isPositive = ticker.priceChangePercent >= 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPositive) CryptoSecondary.copy(alpha = 0.15f) else CryptoError.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) CryptoSecondary else CryptoError,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", ticker.priceChangePercent)}%",
                                color = if (isPositive) CryptoSecondary else CryptoError,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24h High, Low, Vol
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CryptoSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("24h High", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.1f", ticker.highPrice)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("24h Low", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = "$${String.format("%,.1f", ticker.lowPrice)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("24h Vol (BTC)", fontSize = 10.sp, color = CryptoTextMuted)
                    Text(
                        text = String.format("%,.1f", ticker.volume),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CryptoTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartRealTimePnlCard(
    accountBalance: AccountBalance,
    totalRealizedPnl: Double,
    activePositions: List<TradingPosition>,
    currentPrice: Double,
    onSimulateLong: () -> Unit,
    onSimulateShort: () -> Unit,
    onClosePosition: () -> Unit
) {
    val pos = activePositions.firstOrNull()
    val unrealized = accountBalance.unrealizedProfit
    val isProfit = unrealized >= 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (pos != null) {
                if (isProfit) CryptoSecondary.copy(alpha = 0.5f) else CryptoError.copy(alpha = 0.5f)
            } else CryptoBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("chart_realtime_pnl_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "REAL-TIME PNL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CryptoTextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (pos != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (pos.side == SignalType.LONG) CryptoSecondary.copy(alpha = 0.2f) else CryptoError.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pos.side.name} ${pos.quantity} BTC (${pos.leverage}x)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pos.side == SignalType.LONG) CryptoSecondary else CryptoError
                            )
                        }
                    }
                }

                // Close or Simulate action buttons
                if (pos != null) {
                    Button(
                        onClick = onClosePosition,
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoError.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CLOSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onSimulateLong,
                            colors = ButtonDefaults.buttonColors(containerColor = CryptoSecondary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("+ LONG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CryptoSecondary)
                        }
                        Button(
                            onClick = onSimulateShort,
                            colors = ButtonDefaults.buttonColors(containerColor = CryptoError.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("- SHORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CryptoError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live PnL Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Unrealized PnL", fontSize = 10.sp, color = CryptoTextSecondary)
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format("%,.2f", unrealized)} USDT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isProfit) CryptoSecondary else CryptoError,
                        modifier = Modifier.testTag("chart_unrealized_pnl_text")
                    )
                }

                if (pos != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ROI", fontSize = 10.sp, color = CryptoTextSecondary)
                        Text(
                            text = "${if (pos.unrealizedPnlPercent >= 0) "+" else ""}${String.format("%.2f", pos.unrealizedPnlPercent)}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (pos.unrealizedPnlPercent >= 0) CryptoSecondary else CryptoError
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Equity Balance", fontSize = 10.sp, color = CryptoTextSecondary)
                        Text(
                            text = "$${String.format("%,.2f", accountBalance.marginBalance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CryptoTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandlestickChartCard(
    ticker: Ticker,
    candles: List<Candle>,
    indicators: IndicatorState,
    activePositions: List<TradingPosition>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(CryptoPrimary, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EMA 50", fontSize = 11.sp, color = CryptoPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(CryptoPurple, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EMA 200", fontSize = 11.sp, color = CryptoPurple)
                    }
                }

                if (activePositions.isNotEmpty()) {
                    val pos = activePositions.first()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pos.isSlTrailed) "TSL: $${String.format("%.0f", pos.stopLoss)} ${if (pos.isProfitLocked) "🔒" else "📈"}" else "SL: $${String.format("%.0f", pos.stopLoss)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (pos.isProfitLocked) CryptoSecondary else CryptoError,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TP: $${String.format("%.0f", pos.takeProfit)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CryptoSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Candlestick Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090D14))
            ) {
                if (candles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading Live BTC Candlesticks...", color = CryptoTextMuted, fontSize = 12.sp)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().testTag("candlestick_canvas")) {
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
                        val paddingRatio = 0.06
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
                                color = CryptoBorder.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        val candleCount = visibleCandles.size
                        val slotWidth = canvasWidth / candleCount
                        val candleWidth = max(2f, slotWidth * 0.7f)

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

                        // Overlay 50 EMA & 200 EMA lines safely
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
                                    drawPath(
                                        path = path,
                                        color = color,
                                        style = Stroke(width = 2f)
                                    )
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
                                end = Offset(canvasWidth - 65f, currentY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Right price pill
                            drawRect(
                                color = CryptoPrimary,
                                topLeft = Offset(canvasWidth - 65f, currentY - 9f),
                                size = Size(65f, 18f)
                            )
                        }

                        // Draw Active Order Overlay Lines (Entry, SL, TP)
                        activePositions.firstOrNull()?.let { pos ->
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

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

                            // Entry Price (Amber)
                            if (pos.entryPrice > 0.0 && !pos.entryPrice.isNaN() && !pos.entryPrice.isInfinite()) {
                                val entryY = priceToY(pos.entryPrice)
                                if (!entryY.isNaN() && !entryY.isInfinite()) {
                                    drawLine(
                                        color = CryptoWarning,
                                        start = Offset(0f, entryY),
                                        end = Offset(canvasWidth, entryY),
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

@Composable
private fun SubChartCard(
    candles: List<Candle>,
    indicators: IndicatorState,
    selectedMode: String,
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMode == "RSI",
                        onClick = { onSelectMode("RSI") },
                        label = { Text("RSI (14)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CryptoPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CryptoPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedMode == "MACD",
                        onClick = { onSelectMode("MACD") },
                        label = { Text("MACD (12,26,9)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CryptoPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CryptoPrimary
                        )
                    )
                }

                Text(
                    text = if (selectedMode == "RSI")
                        "Value: ${String.format("%.1f", indicators.rsi)}"
                    else "Hist: ${String.format("%.2f", indicators.macdHist)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CryptoTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090D14))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    if (selectedMode == "RSI") {
                        // RSI 30 & 70 threshold lines
                        val y70 = h * (1.0f - 0.70f)
                        val y30 = h * (1.0f - 0.30f)

                        drawLine(
                            color = CryptoError.copy(alpha = 0.5f),
                            start = Offset(0f, y70),
                            end = Offset(w, y70),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawLine(
                            color = CryptoSecondary.copy(alpha = 0.5f),
                            start = Offset(0f, y30),
                            end = Offset(w, y30),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Draw RSI line if history available
                        val closePrices = candles.map { it.close }
                        if (closePrices.size >= 15) {
                            val rsiPoints = mutableListOf<Float>()
                            val count = min(30, closePrices.size - 14)
                            for (idx in (closePrices.size - count) until closePrices.size) {
                                val sub = closePrices.take(idx + 1)
                                val rsiVal = IndicatorCalculator.calculateRSI(sub, 14).toFloat()
                                rsiPoints.add(h * (1.0f - (rsiVal / 100f)))
                            }

                            val stepX = w / (rsiPoints.size - 1).coerceAtLeast(1)
                            val path = Path()
                            for (i in rsiPoints.indices) {
                                val x = i * stepX
                                val y = rsiPoints[i]
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path = path, color = CryptoPrimary, style = Stroke(width = 2f))
                        }
                    } else {
                        // MACD Zero Line
                        val zeroY = h / 2f
                        drawLine(
                            color = CryptoBorder,
                            start = Offset(0f, zeroY),
                            end = Offset(w, zeroY),
                            strokeWidth = 1f
                        )

                        // Histogram bar representation
                        val histVal = indicators.macdHist.toFloat()
                        val barHeight = min(h / 2f - 4f, kotlin.math.abs(histVal) * 20f)
                        val barColor = if (histVal >= 0) CryptoSecondary else CryptoError
                        val top = if (histVal >= 0) zeroY - barHeight else zeroY

                        drawRect(
                            color = barColor,
                            topLeft = Offset(w / 2f - 15f, top),
                            size = Size(30f, max(4f, barHeight))
                        )
                    }
                }
            }
        }
    }
}
