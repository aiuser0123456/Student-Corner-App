package com.studentcorner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colours (mirrors the website's CSS variables) ────────────────────────
val PrimaryBlue = Color(0xFF3F51B5)       // Deep blue – primary
val PrimaryBlueDark = Color(0xFF283593)
val AccentPurple = Color(0xFF7E57C2)      // Vibrant purple – accent
val BackgroundBlue = Color(0xFFE8EAF6)    // Very light blue – background
val SurfaceWhite = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFB00020)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFC5CAE9),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = AccentPurple,
    onSecondary = OnPrimary,
    secondaryContainer = Color(0xFFD1C4E9),
    background = BackgroundBlue,
    surface = SurfaceWhite,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    error = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF1A237E),
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFC5CAE9),
    secondary = Color(0xFFB39DDB),
    onSecondary = Color(0xFF4A148C),
    background = Color(0xFF1A1C2A),
    surface = Color(0xFF22243A),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    error = Color(0xFFCF6679),
)

@Composable
fun StudentCornerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
