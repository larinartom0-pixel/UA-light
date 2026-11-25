package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricYellow,
    onPrimary = Color(0xFF1E1E1E),
    primaryContainer = DarkNavySurfaceVariant,
    onPrimaryContainer = ElectricYellow,
    secondary = ElectricBlue,
    onSecondary = Color(0xFF0F172A),
    background = DarkNavyBg,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkNavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkNavyBorder,
    error = OutageRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
