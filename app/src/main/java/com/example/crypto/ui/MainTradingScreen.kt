package com.example.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.ui.screens.ChartScreen
import com.example.crypto.ui.screens.DashboardScreen
import com.example.crypto.ui.screens.StrategyConfigScreen
import com.example.crypto.ui.screens.TradeLogsScreen
import com.example.ui.theme.CryptoBackground
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoPrimary
import com.example.ui.theme.CryptoSecondary
import com.example.ui.theme.CryptoSurface
import com.example.ui.theme.CryptoTextMuted
import com.example.ui.theme.CryptoTextPrimary

enum class NavigationScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CHART("Live Chart", Icons.Default.CandlestickChart),
    STRATEGY("Strategy", Icons.Default.Tune),
    LOGS("History", Icons.Default.ReceiptLong)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTradingScreen(
    viewModel: CryptoBotViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tradeLogs by viewModel.tradeHistory.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NexusTrade Quant Bot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CryptoTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CryptoSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CryptoSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                NavigationScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CryptoPrimary,
                            indicatorColor = CryptoPrimary,
                            unselectedIconColor = CryptoTextMuted,
                            unselectedTextColor = CryptoTextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = CryptoBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavigationScreen.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onToggleBotActive = { viewModel.toggleBotActive() },
                        onToggleTradingMode = { viewModel.setTradingMode(it) },
                        onEmergencyKillSwitch = { viewModel.triggerEmergencyKillSwitch() },
                        onSimulateSignal = { viewModel.simulateTradeSignal(it) },
                        onTimeframeChange = { viewModel.setTimeframe(it) }
                    )
                }
                NavigationScreen.CHART -> {
                    ChartScreen(
                        ticker = uiState.ticker,
                        candles = uiState.candles,
                        indicators = uiState.indicatorState,
                        activePositions = uiState.activePositions,
                        accountBalance = uiState.accountBalance,
                        totalRealizedPnl = uiState.totalRealizedPnl,
                        priceChangeDirection = uiState.priceChangeDirection,
                        onSimulateLong = { viewModel.simulateTradeSignal(com.example.crypto.domain.model.SignalType.LONG) },
                        onSimulateShort = { viewModel.simulateTradeSignal(com.example.crypto.domain.model.SignalType.SHORT) },
                        onClosePosition = { viewModel.triggerEmergencyKillSwitch() }
                    )
                }
                NavigationScreen.STRATEGY -> {
                    StrategyConfigScreen(
                        currentConfig = uiState.config,
                        isTestingApi = uiState.isTestingApi,
                        apiTestMessage = uiState.apiTestMessage,
                        hasStoredCredentials = uiState.hasStoredCredentials,
                        maskedApiKey = uiState.maskedApiKey,
                        isRealMainnet = uiState.isRealMainnet,
                        devicePublicIp = uiState.devicePublicIp,
                        isFetchingIp = uiState.isFetchingIp,
                        accountBalance = uiState.accountBalance,
                        onSaveConfig = { viewModel.updateConfig(it) },
                        onSaveCredentials = { key, secret, isReal -> viewModel.saveApiCredentials(key, secret, isReal) },
                        onTestApiConnection = { key, secret, isReal -> viewModel.testLiveConnection(key, secret, isReal) },
                        onRefreshIp = { viewModel.fetchDevicePublicIp() },
                        onClearCredentials = { viewModel.clearApiCredentials() },
                        onSwitchToLiveMode = { viewModel.setTradingMode(com.example.crypto.domain.model.TradingMode.LIVE) },
                        onResetPaperWallet = { viewModel.resetPaperWallet() }
                    )
                }
                NavigationScreen.LOGS -> {
                    TradeLogsScreen(
                        tradeLogs = tradeLogs,
                        onClearLogs = { viewModel.clearTradeLogs() }
                    )
                }
            }
        }
    }
}
