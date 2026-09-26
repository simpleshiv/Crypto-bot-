package com.example.crypto.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.crypto.domain.model.TradingMode

/**
 * Foreground Service that keeps the crypto trading bot and WebSocket execution engine
 * running uninterrupted in real-time when the app is minimized, sent to background,
 * or the device screen is turned off.
 */
class TradingBotForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val mode = intent?.getStringExtra(EXTRA_MODE) ?: "PAPER"
                val btcPrice = intent?.getDoubleExtra(EXTRA_PRICE, 85200.0) ?: 85200.0
                val tradesCount = intent?.getIntExtra(EXTRA_TRADES_COUNT, 0) ?: 0
                val pnl = intent?.getDoubleExtra(EXTRA_PNL, 0.0) ?: 0.0
                val leverage = intent?.getIntExtra(EXTRA_LEVERAGE, 20) ?: 20
                val notification = buildNotification(mode, btcPrice, tradesCount, pnl, leverage)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_START -> {
                val mode = intent?.getStringExtra(EXTRA_MODE) ?: "PAPER"
                val btcPrice = intent?.getDoubleExtra(EXTRA_PRICE, 85200.0) ?: 85200.0
                val tradesCount = intent?.getIntExtra(EXTRA_TRADES_COUNT, 0) ?: 0
                val pnl = intent?.getDoubleExtra(EXTRA_PNL, 0.0) ?: 0.0
                val leverage = intent?.getIntExtra(EXTRA_LEVERAGE, 20) ?: 20

                val notification = buildNotification(mode, btcPrice, tradesCount, pnl, leverage)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun buildNotification(
        mode: String,
        btcPrice: Double,
        tradesCount: Int,
        pnl: Double,
        leverage: Int
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pnlText = if (tradesCount > 0) {
            val sign = if (pnl >= 0) "+" else ""
            " | PnL: ${sign}${String.format("%.2f", pnl)} USDT"
        } else {
            ""
        }

        val modeLabel = if (mode.equals("LIVE", ignoreCase = true)) "LIVE REAL-TIME" else "PAPER TRADING"
        val contentText = "BTC: $${String.format("%,.1f", btcPrice)} ($leverage" + "X) | $tradesCount Active$pnlText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusTrade: $modeLabel Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trading Bot Execution Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps real-time crypto trading engine and positions active in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "nexus_trading_bot_engine_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.crypto.action.START_BOT_SERVICE"
        const val ACTION_UPDATE = "com.example.crypto.action.UPDATE_BOT_SERVICE"
        const val ACTION_STOP = "com.example.crypto.action.STOP_BOT_SERVICE"

        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_PRICE = "extra_price"
        const val EXTRA_TRADES_COUNT = "extra_trades_count"
        const val EXTRA_PNL = "extra_pnl"
        const val EXTRA_LEVERAGE = "extra_leverage"

        fun start(
            context: Context,
            mode: TradingMode,
            btcPrice: Double = 85200.0,
            tradesCount: Int = 0,
            pnl: Double = 0.0,
            leverage: Int = 20
        ) {
            val intent = Intent(context, TradingBotForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_PRICE, btcPrice)
                putExtra(EXTRA_TRADES_COUNT, tradesCount)
                putExtra(EXTRA_PNL, pnl)
                putExtra(EXTRA_LEVERAGE, leverage)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun update(
            context: Context,
            mode: TradingMode,
            btcPrice: Double,
            tradesCount: Int,
            pnl: Double,
            leverage: Int = 20
        ) {
            val intent = Intent(context, TradingBotForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_PRICE, btcPrice)
                putExtra(EXTRA_TRADES_COUNT, tradesCount)
                putExtra(EXTRA_PNL, pnl)
                putExtra(EXTRA_LEVERAGE, leverage)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // If service was not running, try start
                start(context, mode, btcPrice, tradesCount, pnl, leverage)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TradingBotForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
