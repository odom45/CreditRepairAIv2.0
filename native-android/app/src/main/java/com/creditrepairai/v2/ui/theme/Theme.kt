package com.creditrepairai.v2.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Navy950 = Color(0xFF07101F)
val Navy900 = Color(0xFF0B172A)
val Navy800 = Color(0xFF142238)
val Blue500 = Color(0xFF2563EB)
val Cyan400 = Color(0xFF22D3EE)
val Slate100 = Color(0xFFF1F5F9)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Emerald400 = Color(0xFF34D399)
val Amber400 = Color(0xFFFBBF24)
val Rose400 = Color(0xFFFB7185)

private val CreditColors = darkColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    secondary = Cyan400,
    onSecondary = Navy950,
    tertiary = Emerald400,
    background = Navy950,
    onBackground = Slate100,
    surface = Navy900,
    onSurface = Slate100,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate300,
    outline = Color(0xFF334155),
    error = Rose400,
)

@Composable
fun CreditRepairTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
    MaterialTheme(colorScheme = CreditColors, content = content)
}
