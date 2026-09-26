package com.example.crypto.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.data.local.BotStorageManager
import com.example.crypto.data.local.TradeDatabase
import com.example.crypto.data.local.TradeLogEntity
import com.example.crypto.data.remote.WsConnectionState
import com.example.crypto.data.repository.TradingRepository
import com.example.crypto.data.security.KeyStoreSecurityManager
import com.example.crypto.data.service.TradingBotForegroundService
import com.example.crypto.domain.model.AccountBalance
import com.example.crypto.domain.model.BotConfig
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.IndicatorState
import com.example.crypto.domain.model.SignalType
import com.example.crypto.domain.model.Ticker
import com.example.crypto.domain.model.TradeSignal
import com.example.crypto.domain.model.TradingMode
import com.example.crypto.domain.model.TradingPosition
import com.example.crypto.domain.strategy.StrategyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TradingUiState(
    val config: BotConfig = BotConfig(),
    val accountBalance: AccountBalance = AccountBalance(),
    val activePositions: List<TradingPosition> = emptyList(),
    val ticker: Ticker = Ticker("BTCUSDT", 85240.0, 1250.0, 1.95, 86100.0, 84200.0, 48210.5),
    val previousPrice: Double = 85240.0,
    val priceChangeDirection: Int = 0, // 1 for tick UP, -1 for tick DOWN, 0 for neutral
    val selectedTimeframe: String = "1m",
    val candles: List<Candle> = emptyList(),
    val indicatorState: IndicatorState = IndicatorState(),
    val latestSignal: TradeSignal? = null,
    val connectionState: WsConnectionState = WsConnectionState.CONNECTED,
    val botLogs: List<String> = emptyList(),
    val totalTrades: Int = 0,
    val winRatePercent: Double = 0.0,
    val totalRealizedPnl: Double = 0.0,
    val isTestingApi: Boolean = false,
    val apiTestMessage: String? = null,
    val hasStoredCredentials: Boolean = false,
    val maskedApiKey: String = "",
    val isRealMainnet: Boolean = true,
    val devicePublicIp: String = "Detecting...",
    val isFetchingIp: Boolean = false
)

class CryptoBotViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = KeyStoreSecurityManager(application)
    private val database = TradeDatabase.getInstance(application)
    private val storageManager = BotStorageManager(application)
    private val repository = TradingRepository(database.tradeDao(), securityManager, storageManager)
    private val strategyEngine = StrategyEngine()

    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    val tradeHistory: StateFlow<List<TradeLogEntity>> = repository.tradeHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val candleBuffer = mutableListOf<Candle>()
    private val candleLock = Any()

    init {
        val savedConfig = storageManager.getBotConfig()
        strategyEngine.updateConfig(savedConfig)

        _uiState.value = _uiState.value.copy(
            config = savedConfig,
            accountBalance = repository.accountBalance.value,
            activePositions = repository.activePositions.value,
            hasStoredCredentials = securityManager.hasCredentials(),
            maskedApiKey = securityManager.getMaskedApiKey(),
            isRealMainnet = securityManager.isRealMainnet()
        )

        // Automatically maintain background foreground service if bot was active or trade is open
        if (savedConfig.isActive || repository.activePositions.value.isNotEmpty()) {
            TradingBotForegroundService.start(
                context = getApplication(),
                mode = savedConfig.tradingMode,
                btcPrice = 85200.0,
                tradesCount = repository.activePositions.value.size,
                pnl = repository.activePositions.value.sumOf { it.unrealizedPnl },
                leverage = savedConfig.leverage
            )
        }

        // Fetch public IP for Binance whitelist guidance
        fetchDevicePublicIp()

        // Initialize with default historical candles
        viewModelScope.launch(Dispatchers.IO) {
            val initialKlines = repository.fetchHistoricalKlines(savedConfig.symbol, 100)
            val snapshot = synchronized(candleLock) {
                candleBuffer.clear()
                candleBuffer.addAll(initialKlines)
                candleBuffer.toList()
            }

            val (indicators, signal) = strategyEngine.evaluate(snapshot)
            _uiState.value = _uiState.value.copy(
                candles = snapshot.takeLast(60),
                indicatorState = indicators,
                latestSignal = signal
            )

            // Connect to real-time streams
            repository.configureTradingMode(savedConfig.tradingMode, savedConfig.symbol)
        }

        // Collect Real-Time Ticker
        viewModelScope.launch {
            repository.tickerFlow.collect { ticker ->
                val prev = _uiState.value.ticker.lastPrice
                val direction = when {
                    ticker.lastPrice > prev -> 1
                    ticker.lastPrice < prev -> -1
                    else -> _uiState.value.priceChangeDirection
                }

                // Real-time update to current live candle close price with thread safety
                val (snapshot, indicators) = synchronized(candleLock) {
                    if (candleBuffer.isNotEmpty()) {
                        val last = candleBuffer.last()
                        val updated = last.copy(
                            close = ticker.lastPrice,
                            high = maxOf(last.high, ticker.lastPrice),
                            low = minOf(last.low, ticker.lastPrice)
                        )
                        candleBuffer[candleBuffer.size - 1] = updated
                        val listCopy = candleBuffer.toList()
                        val (evalIndicators, _) = strategyEngine.evaluate(listCopy)
                        Pair(listCopy, evalIndicators)
                    } else {
                        Pair(null, null)
                    }
                }

                if (snapshot != null && indicators != null) {
                    _uiState.value = _uiState.value.copy(
                        ticker = ticker,
                        previousPrice = prev,
                        priceChangeDirection = direction,
                        candles = snapshot.takeLast(60),
                        indicatorState = indicators
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        ticker = ticker,
                        previousPrice = prev,
                        priceChangeDirection = direction
                    )
                }
            }
        }

        // Collect WebSocket Kline
        viewModelScope.launch {
            repository.klineFlow.collect { candle ->
                handleIncomingCandle(candle)
            }
        }

        // Collect connection status
        viewModelScope.launch {
            repository.wsConnectionState.collect { connState ->
                _uiState.value = _uiState.value.copy(connectionState = connState)
            }
        }

        // Collect repository balance
        viewModelScope.launch {
            repository.accountBalance.collect { balance ->
                _uiState.value = _uiState.value.copy(accountBalance = balance)
            }
        }

        // Collect synced leverage from Binance Futures
        viewModelScope.launch {
            repository.syncedLeverage.collect { lev ->
                if (lev != _uiState.value.config.leverage) {
                    _uiState.value = _uiState.value.copy(
                        config = _uiState.value.config.copy(leverage = lev)
                    )
                }
            }
        }

        // Collect active positions
        viewModelScope.launch {
            repository.activePositions.collect { positions ->
                _uiState.value = _uiState.value.copy(activePositions = positions)
                if (positions.isNotEmpty() || _uiState.value.config.isActive) {
                    TradingBotForegroundService.update(
                        context = getApplication(),
                        mode = _uiState.value.config.tradingMode,
                        btcPrice = _uiState.value.ticker.lastPrice,
                        tradesCount = positions.size,
                        pnl = positions.sumOf { it.unrealizedPnl },
                        leverage = _uiState.value.config.leverage
                    )
                } else if (!_uiState.value.config.isActive) {
                    TradingBotForegroundService.stop(getApplication())
                }
            }
        }

        // Collect bot logs
        viewModelScope.launch {
            repository.botLogMessages.collect { logs ->
                _uiState.value = _uiState.value.copy(botLogs = logs)
            }
        }

        // Observe Trade History to calculate performance statistics
        viewModelScope.launch {
            tradeHistory.collect { trades ->
                val closedTrades = trades.filter { it.status != "OPEN" }
                val totalCount = closedTrades.size
                val winningTrades = closedTrades.filter { it.pnl > 0.0 }.size
                val winRate = if (totalCount > 0) (winningTrades.toDouble() / totalCount) * 100.0 else 0.0
                val totalPnl = closedTrades.sumOf { it.pnl }

                _uiState.value = _uiState.value.copy(
                    totalTrades = totalCount,
                    winRatePercent = winRate,
                    totalRealizedPnl = totalPnl
                )
            }
        }
    }

    private fun handleIncomingCandle(candle: Candle) {
        val (snapshot, indicatorState, signal) = synchronized(candleLock) {
            if (candleBuffer.isEmpty()) {
                candleBuffer.add(candle)
            } else {
                val last = candleBuffer.last()
                if (last.timestamp == candle.timestamp) {
                    candleBuffer[candleBuffer.size - 1] = candle
                } else {
                    candleBuffer.add(candle)
                    if (candleBuffer.size > 250) {
                        candleBuffer.removeAt(0)
                    }
                }
            }
            val listCopy = candleBuffer.toList()
            val (evalState, evalSignal) = strategyEngine.evaluate(listCopy)
            Triple(listCopy, evalState, evalSignal)
        }

        _uiState.value = _uiState.value.copy(
            candles = snapshot.takeLast(60),
            indicatorState = indicatorState,
            latestSignal = signal ?: _uiState.value.latestSignal
        )

        // Automated execution if bot is active and signal triggered
        if (signal != null && _uiState.value.config.isActive) {
            checkAndExecuteSignal(signal)
        }
    }

    private fun checkAndExecuteSignal(signal: TradeSignal) {
        if (_uiState.value.activePositions.isNotEmpty()) return

        val config = _uiState.value.config
        val walletBalance = _uiState.value.accountBalance.totalWalletBalance
        val positionSize = strategyEngine.calculatePositionSize(
            walletBalance = walletBalance,
            entryPrice = signal.price,
            stopLoss = signal.stopLoss,
            riskPercent = config.riskPercentPerTrade,
            leverage = config.leverage
        )

        if (positionSize > 0.0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.executeSignal(signal, positionSize, config)
            }
        }
    }

    fun toggleBotActive() {
        val newActive = !_uiState.value.config.isActive
        val updatedConfig = _uiState.value.config.copy(isActive = newActive)
        _uiState.value = _uiState.value.copy(config = updatedConfig)
        strategyEngine.updateConfig(updatedConfig)
        storageManager.saveBotConfig(updatedConfig)
        repository.logBot(if (newActive) "BOT STARTED: Multi-indicator strategy active in background." else "BOT PAUSED.")

        if (newActive) {
            TradingBotForegroundService.start(
                context = getApplication(),
                mode = updatedConfig.tradingMode,
                btcPrice = _uiState.value.ticker.lastPrice,
                tradesCount = _uiState.value.activePositions.size,
                pnl = _uiState.value.activePositions.sumOf { it.unrealizedPnl },
                leverage = updatedConfig.leverage
            )
        } else if (_uiState.value.activePositions.isEmpty()) {
            TradingBotForegroundService.stop(getApplication())
        }
    }

    fun setTradingMode(mode: TradingMode) {
        if (mode == _uiState.value.config.tradingMode) return
        val updatedConfig = _uiState.value.config.copy(tradingMode = mode)
        _uiState.value = _uiState.value.copy(config = updatedConfig)
        strategyEngine.updateConfig(updatedConfig)
        storageManager.saveTradingMode(mode)
        storageManager.saveBotConfig(updatedConfig)
        repository.configureTradingMode(mode, updatedConfig.symbol)

        TradingBotForegroundService.update(
            context = getApplication(),
            mode = mode,
            btcPrice = _uiState.value.ticker.lastPrice,
            tradesCount = _uiState.value.activePositions.size,
            pnl = _uiState.value.activePositions.sumOf { it.unrealizedPnl },
            leverage = updatedConfig.leverage
        )
    }

    fun updateConfig(newConfig: BotConfig) {
        val oldLev = _uiState.value.config.leverage
        _uiState.value = _uiState.value.copy(config = newConfig)
        strategyEngine.updateConfig(newConfig)
        storageManager.saveBotConfig(newConfig)
        if (oldLev != newConfig.leverage) {
            repository.updateLeverage(newConfig.leverage)
        }
        repository.logBot("Risk and Strategy settings updated (Multiplier: ${newConfig.leverage}X).")

        if (newConfig.isActive || _uiState.value.activePositions.isNotEmpty()) {
            TradingBotForegroundService.update(
                context = getApplication(),
                mode = newConfig.tradingMode,
                btcPrice = _uiState.value.ticker.lastPrice,
                tradesCount = _uiState.value.activePositions.size,
                pnl = _uiState.value.activePositions.sumOf { it.unrealizedPnl },
                leverage = newConfig.leverage
            )
        }
    }

    fun triggerEmergencyKillSwitch() {
        viewModelScope.launch(Dispatchers.IO) {
            // First pause the bot so no new orders are placed
            val pausedConfig = _uiState.value.config.copy(isActive = false)
            _uiState.value = _uiState.value.copy(config = pausedConfig)
            strategyEngine.updateConfig(pausedConfig)
            storageManager.saveBotConfig(pausedConfig)

            repository.triggerEmergencyKillSwitch()
            TradingBotForegroundService.stop(getApplication())
        }
    }

    fun simulateTradeSignal(type: SignalType) {
        viewModelScope.launch(Dispatchers.IO) {
            val price = _uiState.value.ticker.lastPrice.takeIf { it > 0.0 && !it.isNaN() && !it.isInfinite() } ?: 85200.0
            val rawAtr = _uiState.value.indicatorState.atr
            val atr = if (rawAtr > 0.0 && !rawAtr.isNaN() && !rawAtr.isInfinite()) rawAtr else (price * 0.008)
            val slMultiplier = _uiState.value.config.atrMultiplier.coerceAtLeast(0.5)
            val rrRatio = _uiState.value.config.riskRewardRatio.coerceAtLeast(1.0)
            val slDistance = atr * slMultiplier
            val tpDistance = slDistance * rrRatio

            val (sl, tp) = if (type == SignalType.LONG) {
                Pair(max(100.0, price - slDistance), price + tpDistance)
            } else {
                Pair(price + slDistance, max(100.0, price - tpDistance))
            }

            val simulatedSignal = TradeSignal(
                type = type,
                symbol = _uiState.value.config.symbol,
                price = price,
                stopLoss = sl,
                takeProfit = tp,
                atr = atr,
                reasons = listOf(
                    if (type == SignalType.LONG) "Manual Test Golden Cross" else "Manual Test Death Cross",
                    "Trend Strength Confirmed (ADX 28.5)",
                    if (type == SignalType.LONG) "RSI Momentum Oversold (27.2)" else "RSI Momentum Overbought (73.4)",
                    if (type == SignalType.LONG) "MACD Bullish Cross" else "MACD Bearish Cross"
                )
            )

            val qty = strategyEngine.calculatePositionSize(
                walletBalance = _uiState.value.accountBalance.totalWalletBalance,
                entryPrice = price,
                stopLoss = sl,
                riskPercent = _uiState.value.config.riskPercentPerTrade,
                leverage = _uiState.value.config.leverage
            )

            repository.executeSignal(simulatedSignal, qty, _uiState.value.config)
        }
    }

    fun saveApiCredentials(apiKey: String, apiSecret: String, isRealMainnet: Boolean = true) {
        securityManager.encryptAndSaveCredentials(apiKey, apiSecret)
        securityManager.setRealMainnet(isRealMainnet)
        _uiState.value = _uiState.value.copy(
            hasStoredCredentials = securityManager.hasCredentials(),
            maskedApiKey = securityManager.getMaskedApiKey(),
            isRealMainnet = isRealMainnet,
            apiTestMessage = "✅ Credentials encrypted & saved in Android KeyStore (AES-256)"
        )
        repository.logBot("API Credentials securely stored in Android KeyStore (${if (isRealMainnet) "Real Mainnet" else "Testnet"}).")
    }

    fun testLiveConnection(apiKeyOverride: String? = null, apiSecretOverride: String? = null, isRealMainnet: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isTestingApi = true, apiTestMessage = null)
            val (success, message) = repository.testLiveConnection(apiKeyOverride, apiSecretOverride, isRealMainnet)
            _uiState.value = _uiState.value.copy(
                isTestingApi = false,
                apiTestMessage = message,
                hasStoredCredentials = securityManager.hasCredentials(),
                maskedApiKey = securityManager.getMaskedApiKey(),
                isRealMainnet = isRealMainnet ?: securityManager.isRealMainnet()
            )
            repository.logBot("Live Connection Test: $message")
        }
    }

    fun clearApiCredentials() {
        securityManager.clearCredentials()
        _uiState.value = _uiState.value.copy(
            hasStoredCredentials = false,
            maskedApiKey = "",
            apiTestMessage = "API Credentials cleared from KeyStore."
        )
        repository.logBot("API Credentials removed from Android KeyStore.")
    }

    fun clearTradeLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearTradeLogs()
        }
    }

    fun resetPaperWallet() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetPaperBalance(10000.0)
        }
    }

    fun setTimeframe(timeframe: String) {
        if (_uiState.value.selectedTimeframe == timeframe) return
        _uiState.value = _uiState.value.copy(selectedTimeframe = timeframe)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val klines = repository.fetchHistoricalKlines(_uiState.value.config.symbol, 100, timeframe)
                if (klines.isNotEmpty()) {
                    val (snapshot, indicators, signal) = synchronized(candleLock) {
                        candleBuffer.clear()
                        candleBuffer.addAll(klines)
                        val listCopy = candleBuffer.toList()
                        val (evalIndicators, evalSignal) = strategyEngine.evaluate(listCopy)
                        Triple(listCopy, evalIndicators, evalSignal)
                    }
                    _uiState.value = _uiState.value.copy(
                        candles = snapshot.takeLast(60),
                        indicatorState = indicators,
                        latestSignal = signal
                    )
                }
                repository.configureTradingMode(_uiState.value.config.tradingMode, _uiState.value.config.symbol, timeframe)
            } catch (e: Exception) {
                android.util.Log.e("CryptoBotViewModel", "Error setting timeframe $timeframe: ${e.message}", e)
            }
        }
    }

    fun fetchDevicePublicIp() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isFetchingIp = true, devicePublicIp = "Detecting IP...")
            val detected = try {
                val url = java.net.URL("https://api.ipify.org")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "NexusTradeBot/1.0")
                conn.inputStream.bufferedReader().use { it.readText().trim() }
            } catch (e: Exception) {
                try {
                    val url2 = java.net.URL("https://icanhazip.com")
                    val conn2 = url2.openConnection() as java.net.HttpURLConnection
                    conn2.connectTimeout = 4000
                    conn2.readTimeout = 4000
                    conn2.setRequestProperty("User-Agent", "NexusTradeBot/1.0")
                    conn2.inputStream.bufferedReader().use { it.readText().trim() }
                } catch (e2: Exception) {
                    "Unable to detect (Check Internet)"
                }
            }
            _uiState.value = _uiState.value.copy(
                isFetchingIp = false,
                devicePublicIp = detected
            )
            repository.logBot("Outbound Public IP detected: $detected")
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopWebSocket()
    }
}
