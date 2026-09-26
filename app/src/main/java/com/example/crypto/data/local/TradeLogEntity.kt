package com.example.crypto.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_logs")
data class TradeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val side: String, // LONG, SHORT
    val entryPrice: Double,
    val exitPrice: Double = 0.0,
    val quantity: Double,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val status: String, // OPEN, CLOSED_TP, CLOSED_SL, EMERGENCY_CLOSED, MANUALLY_CLOSED
    val triggerReason: String,
    val entryTime: Long = System.currentTimeMillis(),
    val exitTime: Long? = null,
    val isPaper: Boolean = true,
    val rawApiResponse: String? = null
)
