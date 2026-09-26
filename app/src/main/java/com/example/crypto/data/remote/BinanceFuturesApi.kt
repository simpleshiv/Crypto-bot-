package com.example.crypto.data.remote

import com.example.crypto.data.remote.model.BinanceAccountResponse
import com.example.crypto.data.remote.model.BinanceBalanceResponse
import com.example.crypto.data.remote.model.BinanceCancelOrdersResponse
import com.example.crypto.data.remote.model.BinanceChangeLeverageResponse
import com.example.crypto.data.remote.model.BinanceOrderResponse
import com.example.crypto.data.remote.model.BinancePriceResponse
import com.example.crypto.data.remote.model.BinanceServerTimeResponse
import com.example.crypto.data.remote.model.BinanceTicker24hrResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface BinanceFuturesApi {

    @GET("/fapi/v1/time")
    suspend fun getServerTime(): Response<BinanceServerTimeResponse>

    @GET("/fapi/v2/account")
    suspend fun getAccountInfo(
        @Query("recvWindow") recvWindow: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceAccountResponse>

    @GET("/fapi/v2/balance")
    suspend fun getBalances(
        @Query("recvWindow") recvWindow: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<List<BinanceBalanceResponse>>

    @POST("/fapi/v1/order")
    suspend fun placeOrder(
        @Query("symbol") symbol: String,
        @Query("side") side: String, // BUY, SELL
        @Query("type") type: String, // MARKET, STOP_MARKET, TAKE_PROFIT_MARKET
        @Query("quantity") quantity: String?,
        @Query("stopPrice") stopPrice: String?,
        @Query("closePosition") closePosition: Boolean?,
        @Query("workingType") workingType: String?,
        @Query("reduceOnly") reduceOnly: Boolean?,
        @Query("recvWindow") recvWindow: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceOrderResponse>

    @DELETE("/fapi/v1/allOpenOrders")
    suspend fun cancelAllOpenOrders(
        @Query("symbol") symbol: String,
        @Query("recvWindow") recvWindow: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceCancelOrdersResponse>

    @GET("/fapi/v1/klines")
    suspend fun getHistoricalKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1m",
        @Query("limit") limit: Int = 100
    ): Response<List<List<Any>>>

    @GET("/fapi/v1/ticker/24hr")
    suspend fun get24hrTicker(
        @Query("symbol") symbol: String
    ): Response<BinanceTicker24hrResponse>

    @GET("/fapi/v1/ticker/price")
    suspend fun getTickerPrice(
        @Query("symbol") symbol: String
    ): Response<BinancePriceResponse>

    @POST("/fapi/v1/leverage")
    suspend fun changeLeverage(
        @Query("symbol") symbol: String,
        @Query("leverage") leverage: Int,
        @Query("recvWindow") recvWindow: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceChangeLeverageResponse>
}
