package com.example.crypto.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BinanceAccountResponse(
    @Json(name = "totalWalletBalance") val totalWalletBalance: String = "0",
    @Json(name = "availableBalance") val availableBalance: String = "0",
    @Json(name = "totalUnrealizedProfit") val totalUnrealizedProfit: String = "0",
    @Json(name = "totalMarginBalance") val totalMarginBalance: String = "0",
    @Json(name = "positions") val positions: List<BinancePositionResponse>? = null
)

@JsonClass(generateAdapter = true)
data class BinancePositionResponse(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "positionAmt") val positionAmt: String = "0",
    @Json(name = "entryPrice") val entryPrice: String = "0",
    @Json(name = "markPrice") val markPrice: String = "0",
    @Json(name = "unRealizedProfit") val unRealizedProfit: String = "0",
    @Json(name = "leverage") val leverage: String = "1"
)

@JsonClass(generateAdapter = true)
data class BinanceOrderResponse(
    @Json(name = "orderId") val orderId: Long = 0,
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "clientOrderId") val clientOrderId: String = "",
    @Json(name = "price") val price: String = "0",
    @Json(name = "avgPrice") val avgPrice: String = "0",
    @Json(name = "origQty") val origQty: String = "0",
    @Json(name = "executedQty") val executedQty: String = "0",
    @Json(name = "side") val side: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "stopPrice") val stopPrice: String? = null,
    @Json(name = "updateTime") val updateTime: Long = 0
)

@JsonClass(generateAdapter = true)
data class BinanceCancelOrdersResponse(
    @Json(name = "code") val code: Int? = 200,
    @Json(name = "msg") val msg: String? = "The operation of cancel all open order is done."
)

@JsonClass(generateAdapter = true)
data class BinanceTicker24hrResponse(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "lastPrice") val lastPrice: String,
    @Json(name = "priceChange") val priceChange: String,
    @Json(name = "priceChangePercent") val priceChangePercent: String,
    @Json(name = "highPrice") val highPrice: String,
    @Json(name = "lowPrice") val lowPrice: String,
    @Json(name = "volume") val volume: String
)

@JsonClass(generateAdapter = true)
data class BinancePriceResponse(
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "price") val price: String = "0",
    @Json(name = "time") val time: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BinanceServerTimeResponse(
    @Json(name = "serverTime") val serverTime: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BinanceBalanceResponse(
    @Json(name = "accountAlias") val accountAlias: String = "",
    @Json(name = "asset") val asset: String = "",
    @Json(name = "balance") val balance: String = "0",
    @Json(name = "crossWalletBalance") val crossWalletBalance: String = "0",
    @Json(name = "crossUnPnl") val crossUnPnl: String = "0",
    @Json(name = "availableBalance") val availableBalance: String = "0",
    @Json(name = "maxWithdrawAmount") val maxWithdrawAmount: String = "0"
)

@JsonClass(generateAdapter = true)
data class BinanceChangeLeverageResponse(
    @Json(name = "leverage") val leverage: Int = 20,
    @Json(name = "maxNotionalValue") val maxNotionalValue: String = "",
    @Json(name = "symbol") val symbol: String = ""
)
