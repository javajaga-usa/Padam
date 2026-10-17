package com.padam.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Background = Color(0xFF0E0E12)
val Accent = Color(0xFFE8A33D)
val TextPrimary = Color(0xFFF5F1E8)
val TextSecondary = Color(0xFFB4AFA6)

private val PadamColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A1206),
    background = Background,
    onBackground = TextPrimary,
    surface = Color(0xFF1A1A20),
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    border = Accent,
)

@Composable
fun PadamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PadamColors, content = content)
}
