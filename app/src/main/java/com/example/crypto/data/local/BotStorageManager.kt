package com.example.crypto.data.local

import android.content.Context
import com.example.crypto.domain.model.AccountBalance
import com.example.crypto.domain.model.BotConfig
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.model.TradingMode
import com.example.crypto.domain.model.TradingPosition
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust local persistence manager for trading state.
 * Preserves TradingMode (PAPER/LIVE), BotConfig, Account Balance,
 * and Active Positions across app minimizes, backgrounding, and sudden process termination.
 */
class BotStorageManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("nexus_bot_persistence", Context.MODE_PRIVATE)

    fun saveTradingMode(mode: TradingMode) {
        prefs.edit().putString(KEY_TRADING_MODE, mode.name).apply()
    }

    fun getTradingMode(): TradingMode {
        val name = prefs.getString(KEY_TRADING_MODE, TradingMode.PAPER.name)
        return try {
            TradingMode.valueOf(name ?: TradingMode.PAPER.name)
        } catch (e: Exception) {
            TradingMode.PAPER
        }
    }

    fun saveBotConfig(config: BotConfig) {
        prefs.edit()
            .putString(KEY_CFG_MODE, config.tradingMode.name)
            .putString(KEY_CFG_SYMBOL, config.symbol)
            .putBoolean(KEY_CFG_IS_ACTIVE, config.isActive)
            .putFloat(KEY_CFG_RISK_PERCENT, config.riskPercentPerTrade.toFloat())
            .putFloat(KEY_CFG_RISK_REWARD, config.riskRewardRatio.toFloat())
            .putFloat(KEY_CFG_ATR_MULT, config.atrMultiplier.toFloat())
            .putFloat(KEY_CFG_RSI_OVERSOLD, config.rsiOversold.toFloat())
            .putFloat(KEY_CFG_RSI_OVERBOUGHT, config.rsiOverbought.toFloat())
            .putFloat(KEY_CFG_ADX_THRESH, config.adxThreshold.toFloat())
            .putInt(KEY_CFG_EMA_FAST, config.emaFastPeriod)
            .putInt(KEY_CFG_EMA_SLOW, config.emaSlowPeriod)
            .putInt(KEY_CFG_LEVERAGE, config.leverage)
            .putInt(KEY_CFG_MAX_CONCURRENT, config.maxConcurrentTrades)
            .putBoolean(KEY_CFG_TRAILING_STOP, config.isTrailingStopEnabled)
            .putFloat(KEY_CFG_TRAILING_CALLBACK, config.trailingCallbackPercent.toFloat())
            .apply()
    }

    fun getBotConfig(): BotConfig {
        val mode = getTradingMode()
        return BotConfig(
            tradingMode = mode,
            symbol = prefs.getString(KEY_CFG_SYMBOL, "BTCUSDT") ?: "BTCUSDT",
            isActive = prefs.getBoolean(KEY_CFG_IS_ACTIVE, false),
            riskPercentPerTrade = prefs.getFloat(KEY_CFG_RISK_PERCENT, 1.5f).toDouble(),
            riskRewardRatio = prefs.getFloat(KEY_CFG_RISK_REWARD, 2.0f).toDouble(),
            atrMultiplier = prefs.getFloat(KEY_CFG_ATR_MULT, 1.5f).toDouble(),
            rsiOversold = prefs.getFloat(KEY_CFG_RSI_OVERSOLD, 30.0f).toDouble(),
            rsiOverbought = prefs.getFloat(KEY_CFG_RSI_OVERBOUGHT, 70.0f).toDouble(),
            adxThreshold = prefs.getFloat(KEY_CFG_ADX_THRESH, 25.0f).toDouble(),
            emaFastPeriod = prefs.getInt(KEY_CFG_EMA_FAST, 50),
            emaSlowPeriod = prefs.getInt(KEY_CFG_EMA_SLOW, 200),
            leverage = prefs.getInt(KEY_CFG_LEVERAGE, 20),
            maxConcurrentTrades = prefs.getInt(KEY_CFG_MAX_CONCURRENT, 1),
            isTrailingStopEnabled = prefs.getBoolean(KEY_CFG_TRAILING_STOP, true),
            trailingCallbackPercent = prefs.getFloat(KEY_CFG_TRAILING_CALLBACK, 1.0f).toDouble()
        )
    }

    fun savePaperBalance(balance: AccountBalance) {
        if (!balance.isPaper) return
        prefs.edit()
            .putFloat(KEY_PAPER_TOTAL_WALLET, balance.totalWalletBalance.toFloat())
            .putFloat(KEY_PAPER_AVAILABLE, balance.availableBalance.toFloat())
            .putFloat(KEY_PAPER_MARGIN, balance.marginBalance.toFloat())
            .putFloat(KEY_PAPER_UNREALIZED, balance.unrealizedProfit.toFloat())
            .apply()
    }

    fun getPaperBalance(): AccountBalance {
        val total = prefs.getFloat(KEY_PAPER_TOTAL_WALLET, 10000.0f).toDouble()
        val avail = prefs.getFloat(KEY_PAPER_AVAILABLE, 10000.0f).toDouble()
        val margin = prefs.getFloat(KEY_PAPER_MARGIN, total.toFloat()).toDouble()
        val unpnl = prefs.getFloat(KEY_PAPER_UNREALIZED, 0.0f).toDouble()
        return AccountBalance(
            totalWalletBalance = total,
            availableBalance = avail,
            unrealizedProfit = unpnl,
            marginBalance = margin,
            isPaper = true
        )
    }

    fun saveActivePositions(positions: List<TradingPosition>) {
        val jsonArray = JSONArray()
        for (pos in positions) {
            val obj = JSONObject().apply {
                put("id", pos.id)
                put("symbol", pos.symbol)
                put("side", pos.side.name)
                put("entryPrice", pos.entryPrice)
                put("markPrice", pos.markPrice)
                put("quantity", pos.quantity)
                put("leverage", pos.leverage)
                put("stopLoss", pos.stopLoss)
                put("takeProfit", pos.takeProfit)
                put("isPaper", pos.isPaper)
                put("openTime", pos.openTime)
                put("highestPrice", pos.highestPrice)
                put("lowestPrice", pos.lowestPrice)
                put("initialStopLoss", pos.initialStopLoss)
                put("isTrailingSlActive", pos.isTrailingSlActive)
                put("trailingPercent", pos.trailingPercent)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ACTIVE_POSITIONS_JSON, jsonArray.toString()).apply()
    }

    fun getActivePositions(): List<TradingPosition> {
        val jsonString = prefs.getString(KEY_ACTIVE_POSITIONS_JSON, null) ?: return emptyList()
        val result = mutableListOf<TradingPosition>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val entry = obj.getDouble("entryPrice")
                val sl = obj.getDouble("stopLoss")
                val pos = TradingPosition(
                    id = obj.getString("id"),
                    symbol = obj.getString("symbol"),
                    side = try {
                        SignalType.valueOf(obj.getString("side"))
                    } catch (e: Exception) {
                        SignalType.LONG
                    },
                    entryPrice = entry,
                    markPrice = obj.getDouble("markPrice"),
                    quantity = obj.getDouble("quantity"),
                    leverage = obj.optInt("leverage", 20),
                    stopLoss = sl,
                    takeProfit = obj.getDouble("takeProfit"),
                    isPaper = obj.optBoolean("isPaper", true),
                    openTime = obj.optLong("openTime", System.currentTimeMillis()),
                    highestPrice = obj.optDouble("highestPrice", entry),
                    lowestPrice = obj.optDouble("lowestPrice", entry),
                    initialStopLoss = obj.optDouble("initialStopLoss", sl),
                    isTrailingSlActive = obj.optBoolean("isTrailingSlActive", true),
                    trailingPercent = obj.optDouble("trailingPercent", 1.0)
                )
                result.add(pos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun isInitialDemoCreated(): Boolean {
        return prefs.getBoolean(KEY_DEMO_CREATED, false)
    }

    fun setInitialDemoCreated(created: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_CREATED, created).apply()
    }

    companion object {
        private const val KEY_TRADING_MODE = "saved_trading_mode"
        private const val KEY_CFG_MODE = "cfg_mode"
        private const val KEY_CFG_SYMBOL = "cfg_symbol"
        private const val KEY_CFG_IS_ACTIVE = "cfg_is_active"
        private const val KEY_CFG_RISK_PERCENT = "cfg_risk_percent"
        private const val KEY_CFG_RISK_REWARD = "cfg_risk_reward"
        private const val KEY_CFG_ATR_MULT = "cfg_atr_mult"
        private const val KEY_CFG_RSI_OVERSOLD = "cfg_rsi_oversold"
        private const val KEY_CFG_RSI_OVERBOUGHT = "cfg_rsi_overbought"
        private const val KEY_CFG_ADX_THRESH = "cfg_adx_thresh"
        private const val KEY_CFG_EMA_FAST = "cfg_ema_fast"
        private const val KEY_CFG_EMA_SLOW = "cfg_ema_slow"
        private const val KEY_CFG_LEVERAGE = "cfg_leverage"
        private const val KEY_CFG_MAX_CONCURRENT = "cfg_max_concurrent"
        private const val KEY_CFG_TRAILING_STOP = "cfg_trailing_stop"
        private const val KEY_CFG_TRAILING_CALLBACK = "cfg_trailing_callback"
        private const val KEY_PAPER_TOTAL_WALLET = "paper_total_wallet"
        private const val KEY_PAPER_AVAILABLE = "paper_available"
        private const val KEY_PAPER_MARGIN = "paper_margin"
        private const val KEY_PAPER_UNREALIZED = "paper_unrealized"
        private const val KEY_ACTIVE_POSITIONS_JSON = "saved_active_positions_json"
        private const val KEY_DEMO_CREATED = "initial_demo_position_created_v2"
    }
}
