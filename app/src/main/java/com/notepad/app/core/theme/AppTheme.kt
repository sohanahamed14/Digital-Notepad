package com.notepad.app.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String, val previewColor: Color) {
    SYSTEM("System", Color(0xFF6650a4)),
    LIGHT("Light", Color(0xFFFEF7FF)),
    DARK("Dark", Color(0xFF141218));

    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
        background = Color(0xFF141218),
        surface = Color(0xFF1D1B20),
        onPrimary = Color(0xFF381E72),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5)
    )

    fun lightScheme(): ColorScheme = lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        background = Color(0xFFFEF7FF),
        surface = Color(0xFFFEF7FF),
        onPrimary = Color.White,
        onBackground = Color(0xFF1D1B20),
        onSurface = Color(0xFF1D1B20)
    )
}
