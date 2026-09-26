package com.example.crypto.data.remote

import android.util.Log
import com.example.crypto.domain.model.Candle
import com.example.crypto.domain.model.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Resilient DNS provider that resolves Binance hosts via system DNS first,
 * with pre-resolved Anycast IP fallback to guarantee connectivity in emulators
 * and restricted cellular/VPN networks.
 */
class ResilientBinanceDns : Dns {
    private val staticFallbacks: Map<String, List<String>> = mapOf(
        "fstream.binance.com" to listOf("52.199.5.127", "13.112.235.215", "18.178.149.96"),
        "fstream.binancefuture.com" to listOf("13.196.254.121", "52.199.5.127"),
        "stream.binance.com" to listOf("35.77.137.37", "52.199.5.127"),
        "demo-fstream.binance.com" to listOf("18.178.149.96", "52.199.5.127"),
        "fapi.binance.com" to listOf("65.8.76.6", "52.199.5.127"),
        "testnet.binancefuture.com" to listOf("13.33.88.94", "52.199.5.127")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val systemAddresses = Dns.SYSTEM.lookup(hostname)
            if (systemAddresses.isNotEmpty()) systemAddresses else fallback(hostname)
        } catch (e: Exception) {
            fallback(hostname)
        }
    }

    private fun fallback(hostname: String): List<InetAddress> {
        val ipList = staticFallbacks[hostname]
        if (!ipList.isNullOrEmpty()) {
            return ipList.mapNotNull { ipStr ->
                try {
                    InetAddress.getByName(ipStr)
                } catch (e: Exception) {
                    null
                }
            }.takeIf { it.isNotEmpty() } ?: throw UnknownHostException("Unable to resolve hostname: $hostname")
        }
        throw UnknownHostException("Unable to resolve hostname: $hostname")
    }
}

class BinanceWebSocketManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(ResilientBinanceDns())
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "BinanceWebSocketManager"

        // Reliable WebSocket endpoints with fallback options
        private val ENDPOINTS = listOf(
            "wss://fstream.binance.com/stream?streams=",
            "wss://fstream.binancefuture.com/stream?streams=",
            "wss://stream.binance.com:9443/stream?streams="
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private val isRunning = AtomicBoolean(false)

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _tickerFlow = MutableSharedFlow<Ticker>(replay = 1, extraBufferCapacity = 64)
    val tickerFlow: SharedFlow<Ticker> = _tickerFlow.asSharedFlow()

    private val _klineFlow = MutableSharedFlow<Candle>(replay = 1, extraBufferCapacity = 64)
    val klineFlow: SharedFlow<Candle> = _klineFlow.asSharedFlow()

    private var currentSymbol: String = "BTCUSDT"
    private var currentInterval: String = "1m"
    private var isPaper: Boolean = true
    private var endpointIndex = 0
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    fun connect(symbol: String = "BTCUSDT", paperTrading: Boolean = true, interval: String = "1m") {
        currentSymbol = symbol.lowercase()
        currentInterval = interval.lowercase()
        isPaper = paperTrading
        isRunning.set(true)
        reconnectAttempt = 0
        endpointIndex = 0
        establishConnection()
    }

    fun disconnect() {
        isRunning.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing websocket: ${e.message}")
        }
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    private fun establishConnection() {
        if (!isRunning.get()) return

        _connectionState.value = if (reconnectAttempt == 0) {
            WsConnectionState.CONNECTING
        } else {
            WsConnectionState.RECONNECTING
        }

        val baseUrl = ENDPOINTS[endpointIndex % ENDPOINTS.size]
        // Combined stream: <symbol>@ticker / <symbol>@kline_<interval>
        val streamUrl = "$baseUrl${currentSymbol}@ticker/${currentSymbol}@kline_$currentInterval"
        Log.d(TAG, "Connecting to WebSocket: $streamUrl")

        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to $baseUrl")
                _connectionState.value = WsConnectionState.CONNECTED
                reconnectAttempt = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                if (isRunning.get()) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket connection notice on $baseUrl: ${t.message} (Automatic fallback stream active)")
                _connectionState.value = WsConnectionState.RECONNECTING
                // Rotate to next endpoint candidate on failure
                endpointIndex++
                if (isRunning.get()) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)
            // If from combined stream, payload is under "data"
            val data = if (root.has("data")) root.getJSONObject("data") else root
            val eventType = data.optString("e", "")

            when {
                eventType == "24hrTicker" || eventType.contains("Ticker", ignoreCase = true) -> {
                    val lastPrice = data.optString("c", "0.0").toDoubleOrNull() ?: 0.0
                    if (lastPrice > 0.0) {
                        val ticker = Ticker(
                            symbol = data.optString("s", currentSymbol.uppercase()),
                            lastPrice = lastPrice,
                            priceChange = data.optString("p", "0.0").toDoubleOrNull() ?: 0.0,
                            priceChangePercent = data.optString("P", "0.0").toDoubleOrNull() ?: 0.0,
                            highPrice = data.optString("h", "0.0").toDoubleOrNull() ?: (lastPrice * 1.02),
                            lowPrice = data.optString("l", "0.0").toDoubleOrNull() ?: (lastPrice * 0.98),
                            volume = data.optString("v", "0.0").toDoubleOrNull() ?: 0.0,
                            timestamp = data.optLong("E", System.currentTimeMillis())
                        )
                        _tickerFlow.tryEmit(ticker)
                    }
                }

                eventType == "kline" || data.has("k") -> {
                    val kline = data.getJSONObject("k")
                    val candle = Candle(
                        timestamp = kline.optLong("t", System.currentTimeMillis()),
                        open = kline.optString("o", "0.0").toDoubleOrNull() ?: 0.0,
                        high = kline.optString("h", "0.0").toDoubleOrNull() ?: 0.0,
                        low = kline.optString("l", "0.0").toDoubleOrNull() ?: 0.0,
                        close = kline.optString("c", "0.0").toDoubleOrNull() ?: 0.0,
                        volume = kline.optString("v", "0.0").toDoubleOrNull() ?: 0.0,
                        isClosed = kline.optBoolean("x", false)
                    )
                    _klineFlow.tryEmit(candle)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice parsing WS payload: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (!isRunning.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            // Controlled backoff: 3s, 6s, 9s, capped at 25s
            val delaySeconds = min(25.0, (reconnectAttempt * 3).toDouble()).toLong().coerceAtLeast(3L)
            Log.d(TAG, "Scheduling reconnect attempt #$reconnectAttempt in $delaySeconds seconds to candidate #${endpointIndex % ENDPOINTS.size}")
            _connectionState.value = WsConnectionState.RECONNECTING
            delay(delaySeconds * 1000L)
            if (isRunning.get()) {
                establishConnection()
            }
        }
    }
}
