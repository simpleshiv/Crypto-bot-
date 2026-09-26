package com.example.crypto.domain.model

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isClosed: Boolean = true
)

data class Ticker(
    val symbol: String,
    val lastPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalType {
    LONG,
    SHORT,
    NEUTRAL
}

data class TradeSignal(
    val type: SignalType,
    val symbol: String,
    val price: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val atr: Double,
    val reasons: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class IndicatorState(
    val emaFast: Double = 0.0,
    val emaSlow: Double = 0.0,
    val adx: Double = 0.0,
    val rsi: Double = 0.0,
    val macdLine: Double = 0.0,
    val signalLine: Double = 0.0,
    val macdHist: Double = 0.0,
    val atr: Double = 0.0,
    val isGoldenCross: Boolean = false,
    val isDeathCross: Boolean = false,
    val isAdxStrong: Boolean = false,
    val isRsiOversold: Boolean = false,
    val isRsiOverbought: Boolean = false,
    val isMacdBullish: Boolean = false,
    val isMacdBearish: Boolean = false,
    val longIndicatorsMet: Int = 0,
    val shortIndicatorsMet: Int = 0,
    val totalRequired: Int = 3
)

data class TradingPosition(
    val id: String,
    val symbol: String,
    val side: SignalType, // LONG or SHORT
    val entryPrice: Double,
    val markPrice: Double,
    val quantity: Double,
    val leverage: Int,
    val stopLoss: Double,
    val takeProfit: Double,
    val isPaper: Boolean,
    val openTime: Long = System.currentTimeMillis(),
    val highestPrice: Double = entryPrice,
    val lowestPrice: Double = entryPrice,
    val initialStopLoss: Double = stopLoss,
    val isTrailingSlActive: Boolean = true,
    val trailingPercent: Double = 1.0
) {
    val unrealizedPnl: Double
        get() {
            val priceDiff = if (side == SignalType.LONG) markPrice - entryPrice else entryPrice - markPrice
            return priceDiff * quantity
        }

    val unrealizedPnlPercent: Double
        get() {
            if (entryPrice <= 0.0) return 0.0
            val rawChange = if (side == SignalType.LONG) {
                (markPrice - entryPrice) / entryPrice
            } else {
                (entryPrice - markPrice) / entryPrice
            }
            return rawChange * 100.0 * leverage
        }

    val isSlTrailed: Boolean
        get() = if (side == SignalType.LONG) {
            stopLoss > (initialStopLoss + 0.5)
        } else {
            stopLoss < (initialStopLoss - 0.5)
        }

    val isProfitLocked: Boolean
        get() = if (side == SignalType.LONG) {
            stopLoss >= entryPrice
        } else {
            stopLoss <= entryPrice
        }
}

enum class TradingMode {
    PAPER,
    LIVE
}

data class BotConfig(
    val tradingMode: TradingMode = TradingMode.PAPER,
    val symbol: String = "BTCUSDT",
    val isActive: Boolean = false,
    val riskPercentPerTrade: Double = 1.5, // 1% - 2% recommended
    val riskRewardRatio: Double = 2.0,      // e.g. 1:2
    val atrMultiplier: Double = 1.5,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val adxThreshold: Double = 25.0,
    val emaFastPeriod: Int = 50,
    val emaSlowPeriod: Int = 200,
    val leverage: Int = 20,
    val maxConcurrentTrades: Int = 1,
    val isTrailingStopEnabled: Boolean = true,
    val trailingCallbackPercent: Double = 1.0 // Trailing distance callback %
)

data class AccountBalance(
    val totalWalletBalance: Double = 10000.0, // Default for paper trading
    val availableBalance: Double = 10000.0,
    val unrealizedProfit: Double = 0.0,
    val marginBalance: Double = 10000.0,
    val isPaper: Boolean = true
)
