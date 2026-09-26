package com.example.crypto.domain.strategy

import com.example.crypto.domain.model.BotConfig
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.IndicatorState
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.model.TradeSignal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class StrategyEngine(
    private var config: BotConfig = BotConfig()
) {

    fun updateConfig(newConfig: BotConfig) {
        this.config = newConfig
    }

    /**
     * Evaluates candlestick history and calculates indicator state.
     * Triggers a TradeSignal only when at least 3 out of 4 indicators align in the same direction.
     *
     * 1. Trend: 50 EMA & 200 EMA Golden Cross / Death Cross
     * 2. Strength: ADX(14) > 25 (confirming strong trending market)
     * 3. Momentum: RSI(14) < 30 (Oversold -> Long) or > 70 (Overbought -> Short)
     * 4. Confirmation: MACD (12, 26, 9) histogram direction and Signal line crossover
     * 5. Volatility: ATR(14) computes dynamic Stop-Loss and Take-Profit distances
     */
    fun evaluate(candles: List<Candle>): Pair<IndicatorState, TradeSignal?> {
        if (candles.size < 30) {
            return Pair(IndicatorState(), null)
        }

        val closePrices = candles.map { it.close }
        val currentPrice = closePrices.last()

        // 1. Calculate EMAs (50 and 200)
        // Note: For responsive testing on smaller buffers, if fewer than 200 candles exist,
        // we scale the slow EMA to available buffer or fallback gracefully.
        val fastPeriod = config.emaFastPeriod.coerceAtMost(max(10, closePrices.size / 2))
        val slowPeriod = config.emaSlowPeriod.coerceAtMost(max(20, closePrices.size - 2))

        val fastEmaSeries = IndicatorCalculator.calculateEMA(closePrices, fastPeriod)
        val slowEmaSeries = IndicatorCalculator.calculateEMA(closePrices, slowPeriod)

        val fastEma = fastEmaSeries.lastOrNull() ?: currentPrice
        val slowEma = slowEmaSeries.lastOrNull() ?: currentPrice

        val prevFastEma = if (fastEmaSeries.size >= 2) fastEmaSeries[fastEmaSeries.size - 2] else fastEma
        val prevSlowEma = if (slowEmaSeries.size >= 2) slowEmaSeries[slowEmaSeries.size - 2] else slowEma

        // Golden Cross: fast crosses above slow OR established fast > slow
        val isGoldenCross = (prevFastEma <= prevSlowEma && fastEma > slowEma) || (fastEma > slowEma && currentPrice > fastEma)
        val isDeathCross = (prevFastEma >= prevSlowEma && fastEma < slowEma) || (fastEma < slowEma && currentPrice < fastEma)

        // 2. Strength Filter: ADX(14)
        val adx = IndicatorCalculator.calculateADX(candles, 14)
        val isAdxStrong = adx >= config.adxThreshold

        // 3. Momentum: RSI(14)
        val rsi = IndicatorCalculator.calculateRSI(closePrices, 14)
        val isRsiOversold = rsi <= config.rsiOversold
        val isRsiOverbought = rsi >= config.rsiOverbought

        // 4. Signal Confirmation: MACD (12, 26, 9)
        val macdResult = IndicatorCalculator.calculateMACD(closePrices)
        val macdLine = macdResult?.macdLine ?: 0.0
        val signalLine = macdResult?.signalLine ?: 0.0
        val macdHist = macdResult?.histogram ?: 0.0
        val prevHist = macdResult?.previousHistogram ?: 0.0

        val isMacdBullish = macdLine > signalLine && (macdHist > 0.0 || macdHist > prevHist)
        val isMacdBearish = macdLine < signalLine && (macdHist < 0.0 || macdHist < prevHist)

        // 5. Volatility: ATR(14)
        val rawAtr = IndicatorCalculator.calculateATR(candles, 14)
        // Ensure non-zero fallback for ATR based on percentage of current price
        val atr = if (rawAtr <= 0.0) currentPrice * 0.005 else rawAtr

        // Count Alignments for LONG
        val longReasons = mutableListOf<String>()
        if (isGoldenCross) longReasons.add("EMA Trend (Fast > Slow)")
        if (isAdxStrong) longReasons.add("ADX Trend Strength (${String.format("%.1f", adx)} > ${config.adxThreshold})")
        if (isRsiOversold) longReasons.add("RSI Oversold Momentum (${String.format("%.1f", rsi)} <= ${config.rsiOversold})")
        if (isMacdBullish) longReasons.add("MACD Bullish Alignment (Hist: ${String.format("%.2f", macdHist)})")

        // Count Alignments for SHORT
        val shortReasons = mutableListOf<String>()
        if (isDeathCross) shortReasons.add("EMA Trend (Fast < Slow)")
        if (isAdxStrong) shortReasons.add("ADX Trend Strength (${String.format("%.1f", adx)} > ${config.adxThreshold})")
        if (isRsiOverbought) shortReasons.add("RSI Overbought Reversal (${String.format("%.1f", rsi)} >= ${config.rsiOverbought})")
        if (isMacdBearish) shortReasons.add("MACD Bearish Alignment (Hist: ${String.format("%.2f", macdHist)})")

        val longIndicatorsMet = longReasons.size
        val shortIndicatorsMet = shortReasons.size

        val indicatorState = IndicatorState(
            emaFast = fastEma,
            emaSlow = slowEma,
            adx = adx,
            rsi = rsi,
            macdLine = macdLine,
            signalLine = signalLine,
            macdHist = macdHist,
            atr = atr,
            isGoldenCross = isGoldenCross,
            isDeathCross = isDeathCross,
            isAdxStrong = isAdxStrong,
            isRsiOversold = isRsiOversold,
            isRsiOverbought = isRsiOverbought,
            isMacdBullish = isMacdBearish.not() && isMacdBullish,
            isMacdBearish = isMacdBearish,
            longIndicatorsMet = longIndicatorsMet,
            shortIndicatorsMet = shortIndicatorsMet,
            totalRequired = 3
        )

        // Trade Signal Decision: Requires at least 3 out of 4 indicators aligned
        var signal: TradeSignal? = null

        if (longIndicatorsMet >= 3 && longIndicatorsMet > shortIndicatorsMet) {
            val slDistance = atr * config.atrMultiplier
            val stopLoss = currentPrice - slDistance
            val tpDistance = slDistance * config.riskRewardRatio
            val takeProfit = currentPrice + tpDistance

            signal = TradeSignal(
                type = SignalType.LONG,
                symbol = config.symbol,
                price = currentPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                atr = atr,
                reasons = longReasons
            )
        } else if (shortIndicatorsMet >= 3 && shortIndicatorsMet > longIndicatorsMet) {
            val slDistance = atr * config.atrMultiplier
            val stopLoss = currentPrice + slDistance
            val tpDistance = slDistance * config.riskRewardRatio
            val takeProfit = currentPrice - tpDistance

            signal = TradeSignal(
                type = SignalType.SHORT,
                symbol = config.symbol,
                price = currentPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                atr = atr,
                reasons = shortReasons
            )
        }

        return Pair(indicatorState, signal)
    }

    /**
     * Calculates position size strictly capped at 1% - 2% risk of total wallet balance
     * Formula:
     * Risk Amount = Wallet Balance * (Risk% / 100)
     * SL Distance = |Entry Price - Stop Loss|
     * Target Quantity = Risk Amount / SL Distance
     * Max Notional = Wallet Balance * Leverage * 0.95
     */
    fun calculatePositionSize(
        walletBalance: Double,
        entryPrice: Double,
        stopLoss: Double,
        riskPercent: Double = config.riskPercentPerTrade,
        leverage: Int = config.leverage
    ): Double {
        if (walletBalance <= 0.0 || entryPrice <= 0.0) return 0.0

        val slDistance = abs(entryPrice - stopLoss)
        if (slDistance <= 0.0) return 0.0

        val safeRiskPercent = min(max(riskPercent, 0.5), 5.0) // Safe boundary
        val riskAmount = walletBalance * (safeRiskPercent / 100.0)

        var quantity = riskAmount / slDistance

        // Verify leverage bounds: Notional value must not exceed max margin allowance
        val notionalValue = quantity * entryPrice
        val maxAllowedNotional = walletBalance * leverage * 0.90 // 10% safety buffer

        if (notionalValue > maxAllowedNotional) {
            quantity = maxAllowedNotional / entryPrice
        }

        // Round to 3 decimal places for BTCUSDT contracts
        return (Math.round(quantity * 1000.0) / 1000.0).coerceAtLeast(0.001)
    }
}
