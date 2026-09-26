package com.example.crypto.data.security

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Production HMAC-SHA256 request signing utility for Binance Futures REST API endpoints.
 */
object BinanceSigner {

    private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"

    /**
     * Signs the query parameter string with the user's API Secret using HMAC-SHA256.
     * Returns the hex-encoded lower-case signature string expected by Binance Futures.
     */
    fun sign(queryString: String, apiSecret: String): String {
        if (apiSecret.isBlank()) return ""
        try {
            val secretKeySpec = SecretKeySpec(
                apiSecret.toByteArray(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM
            )
            val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
            mac.init(secretKeySpec)
            val hashBytes = mac.doFinal(queryString.toByteArray(StandardCharsets.UTF_8))
            return bytesToHex(hashBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Builds and signs a parameter map including timestamp and optional recvWindow.
     */
    fun createSignedQuery(
        params: Map<String, Any>,
        apiSecret: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val queryBuilder = StringBuilder()
        val sortedParams = params.toSortedMap()

        for ((key, value) in sortedParams) {
            if (queryBuilder.isNotEmpty()) {
                queryBuilder.append("&")
            }
            queryBuilder.append(key).append("=").append(value)
        }

        if (queryBuilder.isNotEmpty()) {
            queryBuilder.append("&")
        }
        queryBuilder.append("timestamp=").append(timestamp)

        val queryString = queryBuilder.toString()
        val signature = sign(queryString, apiSecret)

        return "$queryString&signature=$signature"
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
