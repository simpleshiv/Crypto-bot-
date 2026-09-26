package com.example.crypto.domain.strategy

import com.example.crypto.domain.model.Candle
import kotlin.math.abs
import kotlin.math.max

object IndicatorCalculator {

    /**
     * Calculates Exponential Moving Average (EMA) for a given series of close prices.
     */
    fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()

        val result = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1.0)

        // First EMA is the Simple Moving Average (SMA) of the first 'period' values
        var currentEMA = prices.take(period).average()
        result.add(currentEMA)

        for (i in period until prices.size) {
            currentEMA = (prices[i] - currentEMA) * multiplier + currentEMA
            result.add(currentEMA)
        }
        return result
    }

    /**
     * Calculates Relative Strength Index (RSI) using Wilder's Smoothing.
     */
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size <= period) return 50.0

        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i - 1])
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        // Initial simple average for the first 'period' changes
        for (i in 0 until period) {
            val change = changes[i]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        // Wilder's smoothed averages for subsequent changes
        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }

        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class MacdResult(
        val macdLine: Double,
        val signalLine: Double,
        val histogram: Double,
        val previousHistogram: Double
    )

    /**
     * Calculates MACD (12, 26, 9)
     */
    fun calculateMACD(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdResult? {
        if (prices.size < slowPeriod + signalPeriod) return null

        val fastEmaList = calculateEMA(prices, fastPeriod)
        val slowEmaList = calculateEMA(prices, slowPeriod)

        val offset = slowPeriod - fastPeriod
        val macdLineList = mutableListOf<Double>()

        for (i in slowEmaList.indices) {
            val fastIndex = i + offset
            if (fastIndex < fastEmaList.size) {
                macdLineList.add(fastEmaList[fastIndex] - slowEmaList[i])
            }
        }

        if (macdLineList.size < signalPeriod) return null

        val signalEmaList = calculateEMA(macdLineList, signalPeriod)
        if (signalEmaList.isEmpty()) return null

        val currentMacd = macdLineList.last()
        val currentSignal = signalEmaList.last()
        val currentHist = currentMacd - currentSignal

        val prevHist = if (signalEmaList.size >= 2 && macdLineList.size >= 2) {
            macdLineList[macdLineList.size - 2] - signalEmaList[signalEmaList.size - 2]
        } else {
            currentHist
        }

        return MacdResult(
            macdLine = currentMacd,
            signalLine = currentSignal,
            histogram = currentHist,
            previousHistogram = prevHist
        )
    }

    /**
     * Calculates Average True Range (ATR)
     */
    fun calculateATR(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size <= period) return 0.0

        val trueRanges = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevClose = candles[i - 1].close

            val tr = max(high - low, max(abs(high - prevClose), abs(low - prevClose)))
            trueRanges.add(tr)
        }

        if (trueRanges.size < period) return 0.0

        // Wilder's smoothing for ATR
        var atr = trueRanges.take(period).average()
        for (i in period until trueRanges.size) {
            atr = (atr * (period - 1) + trueRanges[i]) / period
        }
        return atr
    }

    /**
     * Calculates Average Directional Index (ADX) 14
     */
    fun calculateADX(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size <= period * 2) return 20.0

        val trList = mutableListOf<Double>()
        val plusDmList = mutableListOf<Double>()
        val minusDmList = mutableListOf<Double>()

        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevHigh = candles[i - 1].high
            val prevLow = candles[i - 1].low
            val prevClose = candles[i - 1].close

            val tr = max(high - low, max(abs(high - prevClose), abs(low - prevClose)))
            trList.add(tr)

            val upMove = high - prevHigh
            val downMove = prevLow - low

            val plusDm = if (upMove > downMove && upMove > 0) upMove else 0.0
            val minusDm = if (downMove > upMove && downMove > 0) downMove else 0.0

            plusDmList.add(plusDm)
            minusDmList.add(minusDm)
        }

        if (trList.size < period * 2) return 20.0

        var smoothedTr = trList.take(period).sum()
        var smoothedPlusDm = plusDmList.take(period).sum()
        var smoothedMinusDm = minusDmList.take(period).sum()

        val dxList = mutableListOf<Double>()

        for (i in period until trList.size) {
            smoothedTr = smoothedTr - (smoothedTr / period) + trList[i]
            smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / period) + plusDmList[i]
            smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / period) + minusDmList[i]

            val plusDi = if (smoothedTr > 0) (smoothedPlusDm / smoothedTr) * 100.0 else 0.0
            val minusDi = if (smoothedTr > 0) (smoothedMinusDm / smoothedTr) * 100.0 else 0.0

            val diSum = plusDi + minusDi
            val dx = if (diSum > 0) (abs(plusDi - minusDi) / diSum) * 100.0 else 0.0
            dxList.add(dx)
        }

        if (dxList.size < period) return 20.0

        var adx = dxList.take(period).average()
        for (i in period until dxList.size) {
            adx = (adx * (period - 1) + dxList[i]) / period
        }
        return adx
    }
}
