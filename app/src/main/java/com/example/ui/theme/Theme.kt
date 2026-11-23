package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00390E),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFE8F5E9),
    background = MineDarkBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = MineDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = MineDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = MineRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),
    background = MineLightBackground,
    onBackground = Color(0xFF1E293B),
    surface = MineLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = MineLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    error = MineRedDark,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
