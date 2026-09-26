package com.example.crypto.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.data.local.TradeLogEntity
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoError
import com.example.ui.theme.CryptoPrimary
import com.example.ui.theme.CryptoSecondary
import com.example.ui.theme.CryptoSurface
import com.example.ui.theme.CryptoSurfaceVariant
import com.example.ui.theme.CryptoTextMuted
import com.example.ui.theme.CryptoTextPrimary
import com.example.ui.theme.CryptoTextSecondary
import com.example.ui.theme.CryptoWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TradeLogsScreen(
    tradeLogs: List<TradeLogEntity>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredTrades = remember(tradeLogs, selectedFilter) {
        when (selectedFilter) {
            "PAPER" -> tradeLogs.filter { it.isPaper }
            "LIVE" -> tradeLogs.filter { !it.isPaper }
            "TAKE_PROFIT" -> tradeLogs.filter { it.status == "CLOSED_TP" }
            "STOP_LOSS" -> tradeLogs.filter { it.status == "CLOSED_SL" }
            "EMERGENCY" -> tradeLogs.filter { it.status == "EMERGENCY_CLOSED" }
            else -> tradeLogs
        }
    }

    val totalProfit = tradeLogs.filter { it.pnl > 0 }.sumOf { it.pnl }
    val totalLoss = kotlin.math.abs(tradeLogs.filter { it.pnl < 0 }.sumOf { it.pnl })
    val profitFactor = if (totalLoss > 0) totalProfit / totalLoss else if (totalProfit > 0) 99.9 else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats Summary Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM DB TRADE LOGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CryptoTextMuted
                    )

                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(24.dp).testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Logs",
                            tint = CryptoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Logged", fontSize = 11.sp, color = CryptoTextMuted)
                        Text(
                            text = "${tradeLogs.size} trades",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = CryptoTextPrimary
                        )
                    }

                    Column {
                        Text("Gross Profit", fontSize = 11.sp, color = CryptoTextMuted)
                        Text(
                            text = "+$${String.format("%.2f", totalProfit)}",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = CryptoSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Profit Factor", fontSize = 11.sp, color = CryptoTextMuted)
                        Text(
                            text = String.format("%.2f", profitFactor),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (profitFactor >= 1.5) CryptoSecondary else CryptoPrimary
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("ALL", "PAPER", "LIVE", "TAKE_PROFIT", "STOP_LOSS", "EMERGENCY")
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.replace("_", " "), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CryptoPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CryptoPrimary
                    )
                )
            }
        }

        // Trade History List
        if (filteredTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trade logs found for '$selectedFilter'.",
                    color = CryptoTextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("trade_logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTrades, key = { it.id }) { trade ->
                    TradeLogItemCard(trade = trade)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Trade History", color = CryptoTextPrimary) },
            text = { Text("Are you sure you want to permanently delete all trade records from the Room database?", color = CryptoTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearLogs()
                    }
                ) {
                    Text("Clear All", color = CryptoError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = CryptoTextPrimary)
                }
            },
            containerColor = CryptoSurfaceVariant
        )
    }
}

@Composable
private fun TradeLogItemCard(trade: TradeLogEntity) {
    var expanded by remember { mutableStateOf(false) }
    val isProfit = trade.pnl >= 0
    val isLong = trade.side.equals("LONG", ignoreCase = true)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CryptoSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLong) CryptoSecondary.copy(alpha = 0.2f) else CryptoError.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.side,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isLong) CryptoSecondary else CryptoError
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trade.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CryptoTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (trade.isPaper) CryptoPrimary.copy(alpha = 0.15f) else CryptoWarning.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (trade.isPaper) "PAPER" else "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.isPaper) CryptoPrimary else CryptoWarning
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (trade.status == "OPEN") "OPEN"
                        else "${if (isProfit) "+" else ""}${String.format("%.2f", trade.pnl)} USDT (${String.format("%.2f", trade.pnlPercent)}%)",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (trade.status == "OPEN") CryptoWarning
                        else if (isProfit) CryptoSecondary else CryptoError
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = CryptoTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry: $${String.format("%,.2f", trade.entryPrice)} • Exit: ${if (trade.exitPrice > 0) "$${String.format("%,.2f", trade.exitPrice)}" else "--"}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CryptoTextMuted
                )
                Text(
                    text = trade.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (trade.status) {
                        "CLOSED_TP" -> CryptoSecondary
                        "CLOSED_SL" -> CryptoError
                        "EMERGENCY_CLOSED" -> CryptoWarning
                        "OPEN" -> CryptoPrimary
                        else -> CryptoTextMuted
                    }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CryptoSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Text("Trigger Reasons:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CryptoPrimary)
                    Text(trade.triggerReason, fontSize = 11.sp, color = CryptoTextSecondary)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Executed Size: ${trade.quantity} BTC", fontSize = 11.sp, color = CryptoTextSecondary)
                    Text("Entry Timestamp: ${dateFormat.format(Date(trade.entryTime))}", fontSize = 10.sp, color = CryptoTextMuted)
                    if (trade.exitTime != null) {
                        Text("Exit Timestamp: ${dateFormat.format(Date(trade.exitTime))}", fontSize = 10.sp, color = CryptoTextMuted)
                    }

                    if (trade.rawApiResponse != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("API / Engine Status:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CryptoTextMuted)
                        Text(trade.rawApiResponse, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CryptoTextMuted)
                    }
                }
            }
        }
    }
}
