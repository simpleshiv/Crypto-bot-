package com.example

import com.example.crypto.data.security.BinanceSigner
import com.example.crypto.domain.model.BotConfig
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.strategy.IndicatorCalculator
import com.example.crypto.domain.strategy.StrategyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBinanceSignerHmacSha256() {
        val query = "symbol=BTCUSDT&side=BUY&type=LIMIT&quantity=1&price=9000&timeInForce=GTC&timestamp=1578963600000"
        val secret = "nbv98wf56dghjuytr567nbvfrt567nbv"

        val signature = BinanceSigner.sign(query, secret)
        assertNotNull(signature)
        assertTrue(signature.isNotBlank())
        assertEquals(64, signature.length) // Hex representation of SHA256 is 64 chars
    }

    @Test
    fun testIndicatorEmaCalculation() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0)
        val ema5 = IndicatorCalculator.calculateEMA(prices, 5)

        assertTrue(ema5.isNotEmpty())
        assertTrue(ema5.last() > 15.0)
    }

    @Test
    fun testRsiOversoldCalculation() {
        // Falling price series should yield low RSI (< 30)
        val fallingPrices = (100 downTo 50).map { it.toDouble() }
        val rsi = IndicatorCalculator.calculateRSI(fallingPrices, 14)

        assertTrue("RSI should be oversold (< 30) on continuous drop, was $rsi", rsi < 30.0)
    }

    @Test
    fun testRsiOverboughtCalculation() {
        // Rising price series should yield high RSI (> 70)
        val risingPrices = (50..100).map { it.toDouble() }
        val rsi = IndicatorCalculator.calculateRSI(risingPrices, 14)

        assertTrue("RSI should be overbought (> 70) on continuous rally, was $rsi", rsi > 70.0)
    }

    @Test
    fun testRiskCappedPositionSizing() {
        val engine = StrategyEngine(BotConfig(riskPercentPerTrade = 1.5, leverage = 10))
        val wallet = 10000.0
        val entry = 65000.0
        val sl = 64000.0 // Distance = 1000 USDT

        val qty = engine.calculatePositionSize(
            walletBalance = wallet,
            entryPrice = entry,
            stopLoss = sl,
            riskPercent = 1.5,
            leverage = 10
        )

        // 1.5% of 10000 = 150 USDT risk
        // Qty = 150 / 1000 = 0.15 BTC
        assertEquals(0.15, qty, 0.005)
    }
}
