package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CryptoPrimary,
    onPrimary = Color.Black,
    primaryContainer = CryptoSurfaceVariant,
    onPrimaryContainer = CryptoPrimary,
    secondary = CryptoSecondary,
    onSecondary = Color.Black,
    secondaryContainer = CryptoSurfaceVariant,
    onSecondaryContainer = CryptoSecondary,
    tertiary = CryptoPurple,
    onTertiary = Color.Black,
    background = CryptoBackground,
    onBackground = CryptoTextPrimary,
    surface = CryptoSurface,
    onSurface = CryptoTextPrimary,
    surfaceVariant = CryptoSurfaceVariant,
    onSurfaceVariant = CryptoTextSecondary,
    error = CryptoError,
    onError = Color.White,
    outline = CryptoBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce premium crypto dark terminal style
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
