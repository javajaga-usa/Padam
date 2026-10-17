package com.padam.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

val Background = Color(0xFF0B0B0F)
val Surface1 = Color(0xFF17171D)
val Surface2 = Color(0xFF22222A)
val Line = Color(0x14FFFFFF)
val Accent = Color(0xFFF0A73A)
val AccentInk = Color(0xFF1B1206)
val TextPrimary = Color(0xFFF6F2EA)
val TextSecondary = Color(0xFFA9A49B)
val TextFaint = Color(0xFF6F6B64)

private val PadamColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentInk,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    border = Accent,
)

private val Base = Typography()
private val PadamType = Base.copy(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
)

val Eyebrow = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp, color = Accent)

@Composable
fun PadamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PadamColors, typography = PadamType, content = content)
}
