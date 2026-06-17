package com.agentapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === 可爱粉紫色调 ===
val Pink = Color(0xFFFF9EB5)
val PinkDark = Color(0xFFE86A8A)
val PinkLight = Color(0xFFFFE0E6)
val Purple = Color(0xFFB5A8D5)
val PurpleLight = Color(0xFFF0E8FF)
val Coral = Color(0xFFFF6B8A)
val WarmWhite = Color(0xFFFFF5F7)
val TextDark = Color(0xFF3D2C3D)
val TextGray = Color(0xFF9E8E9E)
val ChatBubbleUser = Color(0xFFFFE0E6)
val ChatBubbleAssistant = Color(0xFFF0E8FF)
val DarkBg = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF12121E)
val DarkBubbleUser = Color(0xFF2D1520)
val DarkBubbleAssist = Color(0xFF1D1530)

val AvatarColors = listOf(
    Color(0xFFFF9EB5), Color(0xFFB5A8D5), Color(0xFFFFB347),
    Color(0xFF78C6D3), Color(0xFFA8D8A8), Color(0xFFD4A5D4),
    Color(0xFFFF9AA2), Color(0xFFB0D0E8),
)

private val CuteDark = darkColorScheme(
    primary = Color(0xFFFFB3C1), onPrimary = Color(0xFF4A1A2A),
    primaryContainer = Color(0xFF6B2E42), onPrimaryContainer = Color(0xFFFFE0E6),
    secondary = Color(0xFFD0BCFF), onSecondary = Color(0xFF2D1B4E),
    secondaryContainer = Color(0xFF4A3F6B), onSecondaryContainer = Color(0xFFF0E8FF),
    surface = DarkBg, onSurface = Color(0xFFE8DDE8),
    surfaceVariant = Color(0xFF2D2D42), onSurfaceVariant = Color(0xFFCAC4D0),
    background = DarkSurface, onBackground = Color(0xFFE8DDE8),
    error = Color(0xFFFF6B8A), onError = Color.White,
    outline = Color(0xFF4A3F5C),
)

private val CuteLight = lightColorScheme(
    primary = PinkDark, onPrimary = Color.White,
    primaryContainer = PinkLight, onPrimaryContainer = Color(0xFF4A1A2A),
    secondary = Purple, onSecondary = Color.White,
    secondaryContainer = PurpleLight, onSecondaryContainer = Color(0xFF2D1B4E),
    surface = WarmWhite, onSurface = TextDark,
    surfaceVariant = Color(0xFFF5F0F3), onSurfaceVariant = TextGray,
    background = WarmWhite, onBackground = TextDark,
    error = Color(0xFFE86A8A), onError = Color.White,
    outline = Color(0xFFE8DDE8),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun AgentAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (isDark) CuteDark else CuteLight,
        content = content
    )
}
