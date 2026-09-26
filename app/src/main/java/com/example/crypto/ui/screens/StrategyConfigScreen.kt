package com.example.crypto.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.crypto.domain.model.AccountBalance
import com.example.crypto.domain.model.BotConfig
import com.example.ui.theme.CryptoBorder
import com.example.ui.theme.CryptoError
import com.example.ui.theme.CryptoPrimary
import com.example.ui.theme.CryptoSecondary
import com.example.ui.theme.CryptoSurface
import com.example.ui.theme.CryptoSurfaceBright
import com.example.ui.theme.CryptoSurfaceVariant
import com.example.ui.theme.CryptoTextMuted
import com.example.ui.theme.CryptoTextPrimary
import com.example.ui.theme.CryptoTextSecondary
import com.example.ui.theme.CryptoWarning

@Composable
fun StrategyConfigScreen(
    currentConfig: BotConfig,
    isTestingApi: Boolean,
    apiTestMessage: String?,
    hasStoredCredentials: Boolean,
    maskedApiKey: String = "",
    isRealMainnet: Boolean = true,
    devicePublicIp: String = "Detecting...",
    isFetchingIp: Boolean = false,
    accountBalance: AccountBalance = AccountBalance(),
    onSaveConfig: (BotConfig) -> Unit,
    onSaveCredentials: (String, String, Boolean) -> Unit,
    onTestApiConnection: (String, String, Boolean) -> Unit,
    onRefreshIp: () -> Unit = {},
    onClearCredentials: () -> Unit = {},
    onSwitchToLiveMode: () -> Unit = {},
    onResetPaperWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isIpCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isIpCopied) {
        if (isIpCopied) {
            delay(2500)
            isIpCopied = false
        }
    }

    var riskPercent by remember(currentConfig) { mutableDoubleStateOf(currentConfig.riskPercentPerTrade) }
    var riskReward by remember(currentConfig) { mutableDoubleStateOf(currentConfig.riskRewardRatio) }
    var atrMultiplier by remember(currentConfig) { mutableDoubleStateOf(currentConfig.atrMultiplier) }
    var leverage by remember(currentConfig) { mutableIntStateOf(currentConfig.leverage) }
    var adxThreshold by remember(currentConfig) { mutableDoubleStateOf(currentConfig.adxThreshold) }
    var rsiOversold by remember(currentConfig) { mutableDoubleStateOf(currentConfig.rsiOversold) }
    var rsiOverbought by remember(currentConfig) { mutableDoubleStateOf(currentConfig.rsiOverbought) }
    var isTrailingStopEnabled by remember(currentConfig) { mutableStateOf(currentConfig.isTrailingStopEnabled) }
    var trailingCallbackPercent by remember(currentConfig) { mutableDoubleStateOf(currentConfig.trailingCallbackPercent) }

    var apiKeyInput by remember { mutableStateOf("") }
    var apiSecretInput by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var showSavedSnackbar by remember { mutableStateOf(false) }
    var selectedIsReal by remember(isRealMainnet) { mutableStateOf(isRealMainnet) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Android KeyStore Binance API Vault (PROMINENT AT TOP)
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (hasStoredCredentials) CryptoSecondary.copy(alpha = 0.4f) else CryptoBorder),
            modifier = Modifier.fillMaxWidth().testTag("api_vault_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CryptoWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BINANCE API KEY VAULT",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = CryptoTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasStoredCredentials) CryptoSecondary.copy(alpha = 0.2f) else CryptoSurfaceBright
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (hasStoredCredentials) "AES-256 SECURED" else "NOT CONFIGURED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasStoredCredentials) CryptoSecondary else CryptoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Keys are securely encrypted on-device via Android KeyStore (hardware-backed AES-256-GCM). Used for signed Binance Futures REST calls and position management.",
                    fontSize = 12.sp,
                    color = CryptoTextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Environment selector (Real Mainnet vs Testnet)
                Text(
                    text = "SELECT API ENVIRONMENT:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CryptoTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedIsReal,
                        onClick = { selectedIsReal = true },
                        label = {
                            Text(
                                "Real Mainnet (fapi.binance.com)",
                                fontSize = 11.sp,
                                fontWeight = if (selectedIsReal) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CryptoPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CryptoPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("select_real_mainnet")
                    )

                    FilterChip(
                        selected = !selectedIsReal,
                        onClick = { selectedIsReal = false },
                        label = {
                            Text(
                                "Futures Testnet",
                                fontSize = 11.sp,
                                fontWeight = if (!selectedIsReal) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CryptoWarning.copy(alpha = 0.2f),
                            selectedLabelColor = CryptoWarning
                        ),
                        modifier = Modifier.weight(0.7f).testTag("select_testnet")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Show active stored key badge if saved
                if (hasStoredCredentials && maskedApiKey.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CryptoSurfaceBright)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CryptoSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Active Stored Key:", fontSize = 10.sp, color = CryptoTextMuted)
                                Text(maskedApiKey, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CryptoTextPrimary)
                            }
                        }

                        IconButton(
                            onClick = {
                                onClearCredentials()
                                apiKeyInput = ""
                                apiSecretInput = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Keys", tint = CryptoError, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(if (selectedIsReal) "Real Binance Futures API Key" else "Testnet API Key") },
                    placeholder = { Text(if (maskedApiKey.isNotBlank()) "Enter new key to replace" else "Paste API Key here") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoPrimary,
                        unfocusedBorderColor = CryptoBorder,
                        focusedLabelColor = CryptoPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_key_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiSecretInput,
                    onValueChange = { apiSecretInput = it },
                    label = { Text(if (selectedIsReal) "Real Binance Futures API Secret" else "Testnet API Secret") },
                    placeholder = { Text(if (hasStoredCredentials) "••••••••••••••••••••••••" else "Paste API Secret here") },
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showSecret = !showSecret }) {
                            Icon(
                                imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = CryptoTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CryptoPrimary,
                        unfocusedBorderColor = CryptoBorder,
                        focusedLabelColor = CryptoPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_secret_field")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank() && apiSecretInput.isNotBlank()) {
                                onSaveCredentials(apiKeyInput.trim(), apiSecretInput.trim(), selectedIsReal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoSurfaceBright),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("save_keys_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = CryptoTextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Store in KeyStore", color = CryptoTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // Test immediately using entered keys, or fall back to stored credentials
                            onTestApiConnection(apiKeyInput.trim(), apiSecretInput.trim(), selectedIsReal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CryptoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("test_api_button")
                    ) {
                        if (isTestingApi) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Test Connection", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Response Message Card
                if (apiTestMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val isSuccess = apiTestMessage.contains("Connected") || apiTestMessage.contains("✅")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) CryptoSecondary.copy(alpha = 0.12f) else CryptoError.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSuccess) CryptoSecondary.copy(alpha = 0.5f) else CryptoError.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = apiTestMessage,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                                color = if (isSuccess) CryptoSecondary else CryptoError,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Quick Switch to Live button when connected
                            if (isSuccess && selectedIsReal) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onSwitchToLiveMode,
                                    colors = ButtonDefaults.buttonColors(containerColor = CryptoSecondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("activate_live_trading_button")
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ACTIVATE REAL LIVE TRADING", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1B: Outbound IP Whitelist Helper (With Copy Feature)
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CryptoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("ip_whitelist_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lan,
                            contentDescription = null,
                            tint = CryptoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BINANCE IP WHITELIST HELPER",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = CryptoTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onRefreshIp,
                        modifier = Modifier.size(28.dp).testTag("refresh_ip_button")
                    ) {
                        if (isFetchingIp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CryptoPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh IP",
                                tint = CryptoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aapka device is public IP se Binance server se connect karta hai. Agar aapne Binance API me 'Restrict access to trusted IPs only' select kiya hai, toh ye IP copy karke Binance me paste karein.",
                    fontSize = 12.sp,
                    color = CryptoTextMuted,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Public IP Display Box with One-Tap Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CryptoSurfaceBright)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT DEVICE OUTBOUND IP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CryptoTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = devicePublicIp,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = CryptoSecondary,
                            modifier = Modifier.testTag("device_public_ip_text")
                        )
                    }

                    Button(
                        onClick = {
                            if (devicePublicIp.isNotBlank() &&
                                !devicePublicIp.contains("Detecting") &&
                                !devicePublicIp.contains("Unable")
                            ) {
                                clipboardManager.setText(AnnotatedString(devicePublicIp))
                                isIpCopied = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIpCopied) CryptoSecondary else CryptoPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp).testTag("copy_ip_button")
                    ) {
                        Icon(
                            imageVector = if (isIpCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isIpCopied) "COPIED!" else "COPY IP",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Helpful instructions box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CryptoSurfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Binance API Settings Tips:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CryptoWarning
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Error -2015 aane par: Binance API me 'Enable Futures' permission tick karein aur IP restriction me upar wala IP daalein.\n• Agar 'Unrestricted (Less Secure)' chuna hai, toh koi IP daalne ki zaroorat nahi hai.\n• Wi-Fi se Mobile Data switch karne par IP change ho sakta hai, toh 'Refresh' daba kar naya IP check karein.",
                        fontSize = 11.sp,
                        color = CryptoTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section 2: Risk Management Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CryptoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RISK MANAGEMENT PROTOCOL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CryptoTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Risk % per trade slider (1% - 5%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Capital Risk Per Trade", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "${String.format("%.1f", riskPercent)}%",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (riskPercent <= 2.0) CryptoSecondary else CryptoWarning
                    )
                }
                Slider(
                    value = riskPercent.toFloat(),
                    onValueChange = { riskPercent = (Math.round(it * 10.0) / 10.0) },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoPrimary,
                        activeTrackColor = CryptoPrimary,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Target Risk-to-Reward Ratio slider (1:1.0 - 1:4.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Risk-to-Reward Ratio", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "1 : ${String.format("%.1f", riskReward)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CryptoPrimary
                    )
                }
                Slider(
                    value = riskReward.toFloat(),
                    onValueChange = { riskReward = (Math.round(it * 10.0) / 10.0) },
                    valueRange = 1.0f..4.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoPrimary,
                        activeTrackColor = CryptoPrimary,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic ATR Multiplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ATR Stop-Loss Distance Multiplier", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "${String.format("%.1f", atrMultiplier)}x ATR",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CryptoTextPrimary
                    )
                }
                Slider(
                    value = atrMultiplier.toFloat(),
                    onValueChange = { atrMultiplier = (Math.round(it * 10.0) / 10.0) },
                    valueRange = 1.0f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoPrimary,
                        activeTrackColor = CryptoPrimary,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-Trailing Stop Loss (Profit Lock) Module
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CryptoSurfaceVariant.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isTrailingStopEnabled) CryptoSecondary else CryptoTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Trailing Stop Loss",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTrailingStopEnabled) CryptoSecondary else CryptoTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Trade badhne par SL automatically aage badhta rahega, aur market reverse/niche aane par naye SL par trade close ho jayega.",
                                fontSize = 10.sp,
                                color = CryptoTextSecondary,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isTrailingStopEnabled,
                            onCheckedChange = { isTrailingStopEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CryptoSecondary,
                                uncheckedThumbColor = CryptoTextMuted,
                                uncheckedTrackColor = CryptoSurfaceBright
                            ),
                            modifier = Modifier.testTag("trailing_sl_switch")
                        )
                    }

                    if (isTrailingStopEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CryptoSecondary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ Auto-Profit Lock: Jab trade profit me jayega, SL move hokar entry se upar aa jayega taaki profit safe rahe.",
                                fontSize = 10.sp,
                                color = CryptoSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Leverage selection (1x - 50x) with quick preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Futures Leverage Multiplier", fontSize = 12.sp, color = CryptoTextSecondary)
                        Text("Binance Account Multiplier: ${currentConfig.leverage}X", fontSize = 10.sp, color = CryptoSecondary)
                    }
                    Text(
                        text = "${leverage}X",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        color = if (leverage <= 20) CryptoSecondary else CryptoWarning
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Preset Chips (5x, 10x, 20x Binance default, 50x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 20, 50).forEach { preset ->
                        FilterChip(
                            selected = leverage == preset,
                            onClick = { leverage = preset },
                            label = {
                                Text(
                                    text = if (preset == 20) "20x (Binance Default)" else "${preset}x",
                                    fontSize = 11.sp,
                                    fontWeight = if (leverage == preset) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (preset == 20) CryptoSecondary.copy(alpha = 0.25f) else CryptoPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = if (preset == 20) CryptoSecondary else CryptoPrimary
                            ),
                            modifier = Modifier.testTag("leverage_preset_${preset}x")
                        )
                    }
                }

                Slider(
                    value = leverage.toFloat(),
                    onValueChange = { leverage = it.toInt() },
                    valueRange = 1f..50f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = if (leverage <= 20) CryptoSecondary else CryptoWarning,
                        activeTrackColor = if (leverage <= 20) CryptoSecondary else CryptoWarning,
                        inactiveTrackColor = CryptoSurfaceBright
                    ),
                    modifier = Modifier.testTag("leverage_slider")
                )
            }
        }

        // Section 3: Strategy Indicator Parameters
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CryptoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INDICATOR ENGINE THRESHOLDS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CryptoTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ADX Trend Strength Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ADX(14) Trend Strength Min", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "> ${adxThreshold.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CryptoPrimary
                    )
                }
                Slider(
                    value = adxThreshold.toFloat(),
                    onValueChange = { adxThreshold = it.toDouble() },
                    valueRange = 20f..35f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoPrimary,
                        activeTrackColor = CryptoPrimary,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // RSI Oversold threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RSI(14) Oversold Level (Long)", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "< ${rsiOversold.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CryptoSecondary
                    )
                }
                Slider(
                    value = rsiOversold.toFloat(),
                    onValueChange = { rsiOversold = it.toDouble() },
                    valueRange = 20f..35f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoSecondary,
                        activeTrackColor = CryptoSecondary,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // RSI Overbought threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RSI(14) Overbought Level (Short)", fontSize = 12.sp, color = CryptoTextSecondary)
                    Text(
                        text = "> ${rsiOverbought.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CryptoError
                    )
                }
                Slider(
                    value = rsiOverbought.toFloat(),
                    onValueChange = { rsiOverbought = it.toDouble() },
                    valueRange = 65f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = CryptoError,
                        activeTrackColor = CryptoError,
                        inactiveTrackColor = CryptoSurfaceBright
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val updated = currentConfig.copy(
                            riskPercentPerTrade = riskPercent,
                            riskRewardRatio = riskReward,
                            atrMultiplier = atrMultiplier,
                            leverage = leverage,
                            adxThreshold = adxThreshold,
                            rsiOversold = rsiOversold,
                            rsiOverbought = rsiOverbought,
                            isTrailingStopEnabled = isTrailingStopEnabled,
                            trailingCallbackPercent = trailingCallbackPercent
                        )
                        onSaveConfig(updated)
                        showSavedSnackbar = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CryptoPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_config_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE STRATEGY CONFIGURATION", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 4: Paper Wallet Reset
        Card(
            colors = CardDefaults.cardColors(containerColor = CryptoSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reset Paper Wallet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CryptoTextPrimary)
                    Text("Restore virtual balance to $10,000 USDT", fontSize = 12.sp, color = CryptoTextMuted)
                }

                OutlinedButton(
                    onClick = onResetPaperWallet,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CryptoPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("reset_paper_wallet_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}
