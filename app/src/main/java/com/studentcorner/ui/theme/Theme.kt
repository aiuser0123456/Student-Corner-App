package com.studentcorner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand palette ──────────────────────────────────────────────────────────────
val Brand100  = Color(0xFFE8EAF6)
val Brand200  = Color(0xFFC5CAE9)
val Brand400  = Color(0xFF7986CB)
val Brand500  = Color(0xFF3F51B5)   // primary
val Brand700  = Color(0xFF283593)
val Accent400 = Color(0xFFB39DDB)
val Accent500 = Color(0xFF7E57C2)   // secondary
val Accent700 = Color(0xFF4527A0)

// Light surfaces
val SurfaceLight  = Color(0xFFFFFFFF)
val BgLight       = Color(0xFFF4F5FA)
val CardLight     = Color(0xFFFFFFFF)

// Dark surfaces
val BgDark        = Color(0xFF0F1117)
val SurfaceDark   = Color(0xFF1A1D2E)
val CardDark      = Color(0xFF222539)
val OnBgDark      = Color(0xFFE4E6F1)

private val LightColors = lightColorScheme(
    primary              = Brand500,
    onPrimary            = Color.White,
    primaryContainer     = Brand100,
    onPrimaryContainer   = Brand700,
    secondary            = Accent500,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEDE7F6),
    onSecondaryContainer = Accent700,
    tertiary             = Color(0xFF00897B),
    background           = BgLight,
    onBackground         = Color(0xFF1A1C2E),
    surface              = SurfaceLight,
    onSurface            = Color(0xFF1A1C2E),
    surfaceVariant       = Color(0xFFEEEFF8),
    onSurfaceVariant     = Color(0xFF5C5F72),
    outline              = Color(0xFFBBBDCC),
    error                = Color(0xFFB00020),
)

private val DarkColors = darkColorScheme(
    primary              = Brand400,
    onPrimary            = Color(0xFF1A237E),
    primaryContainer     = Brand700,
    onPrimaryContainer   = Brand200,
    secondary            = Accent400,
    onSecondary          = Color(0xFF2D1B69),
    secondaryContainer   = Accent700,
    onSecondaryContainer = Accent400,
    tertiary             = Color(0xFF4DB6AC),
    background           = BgDark,
    onBackground         = OnBgDark,
    surface              = SurfaceDark,
    onSurface            = OnBgDark,
    surfaceVariant       = CardDark,
    onSurfaceVariant     = Color(0xFFA0A3B8),
    outline              = Color(0xFF3A3D52),
    error                = Color(0xFFCF6679),
)

@Composable
fun StudentCornerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
