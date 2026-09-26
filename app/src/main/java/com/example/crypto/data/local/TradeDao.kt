package com.example.crypto.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    @Query("SELECT * FROM trade_logs ORDER BY entryTime DESC")
    fun getAllTrades(): Flow<List<TradeLogEntity>>

    @Query("SELECT * FROM trade_logs WHERE isPaper = :isPaper ORDER BY entryTime DESC")
    fun getTradesByMode(isPaper: Boolean): Flow<List<TradeLogEntity>>

    @Query("SELECT * FROM trade_logs WHERE status = 'OPEN' ORDER BY entryTime DESC")
    fun getOpenTrades(): Flow<List<TradeLogEntity>>

    @Query("SELECT * FROM trade_logs WHERE id = :id LIMIT 1")
    suspend fun getTradeById(id: Long): TradeLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeLogEntity): Long

    @Update
    suspend fun updateTrade(trade: TradeLogEntity)

    @Query("DELETE FROM trade_logs WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("DELETE FROM trade_logs")
    suspend fun clearAllTrades()
}
