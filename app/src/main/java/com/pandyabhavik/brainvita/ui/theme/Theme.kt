package com.pandyabhavik.brainvita.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Walnut board on a deep green baize, brass pegs. The same palette day and night: a games table has one look. */
object Palette {
    val baize = Color(0xFF14241B)
    val baizeLight = Color(0xFF1E3528)
    val walnut = Color(0xFF5A3A24)
    val walnutLight = Color(0xFF7B5234)
    val walnutDark = Color(0xFF3A2416)
    val grain = Color(0x2A1A0E08)
    val holeDeep = Color(0xFF1A0F08)
    val holeShadow = Color(0xFF2B1A0F)
    val holeLip = Color(0x66FFE0B8)
    val brass = Color(0xFFE6C27A)
    val brassLight = Color(0xFFFFF0C2)
    val brassDark = Color(0xFF9C7230)
    val glow = Color(0xFFA8F0C6)
    val ivory = Color(0xFFF4ECDC)
    val ivoryDim = Color(0xFFB9B09C)
    val danger = Color(0xFFE09A8A)
}

private val scheme = darkColorScheme(
    primary = Palette.brass,
    onPrimary = Palette.walnutDark,
    secondary = Palette.glow,
    background = Palette.baize,
    onBackground = Palette.ivory,
    surface = Palette.baize,
    onSurface = Palette.ivory,
    surfaceVariant = Palette.baizeLight,
    onSurfaceVariant = Palette.ivoryDim,
    outline = Palette.brassDark,
)

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 34.sp, letterSpacing = 1.5.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.sp),
)

@Composable
fun BrainVitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
