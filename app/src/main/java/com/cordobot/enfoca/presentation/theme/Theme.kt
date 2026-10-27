package com.cordobot.enfoca.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Coral = Color(0xFFE76F51)
val Teal = Color(0xFF2A9D8F)

/** Colors a habit can take. */
val HabitColors = listOf(0xFFE76F51, 0xFF2A9D8F, 0xFFE9C46A, 0xFF8E7DBE, 0xFF4EA8DE, 0xFFF28482)

private val colors = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Teal,
    background = Color(0xFF101418),
    onBackground = Color.White,
    surface = Color(0xFF1A2026),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF242C34),
    onSurfaceVariant = Color(0xFFA7B1BC),
    surfaceContainer = Color(0xFF161B21)
)

private val typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
)

@Composable
fun EnfocaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
