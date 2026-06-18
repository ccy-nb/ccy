package com.agentapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === SillyTavern 风格配色 ===
// 深色：深蓝灰底，暖白字，暗橙/蓝点缀
// 浅色：米白底，暖灰字，保留粉紫 accent

// === 深色模式 ===
val DarkBg = Color(0xFF1A1B26)
val DarkSurface = Color(0xFF24253A)
val DarkSurfaceVariant = Color(0xFF2D2E42)
val DarkOnSurface = Color(0xFFC9D1D9)
val DarkOnSurfaceDim = Color(0xFF8B949E)
val DarkBubbleAssist = Color(0xFF2D2E42)
val DarkBubbleUser = Color(0xFF1F3A5F)

// === 浅色模式 ===
val LightBg = Color(0xFFFAFAF8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3F0EB)
val LightOnSurface = Color(0xFF3B3B3B)
val LightOnSurfaceDim = Color(0xFF8B8B8B)
val LightBubbleAssist = Color(0xFFF0EDE8)
val LightBubbleUser = Color(0xFFD6E4F0)

// === 共用强调色 ===
val AccentOrange = Color(0xFFE06C75)
val AccentBlue = Color(0xFF61AFEF)
val AccentGreen = Color(0xFF98C379)
val AccentYellow = Color(0xFFE5C07B)
val AccentPurple = Color(0xFFC678DD)
val AccentCyan = Color(0xFF56B6C2)

// === 保留粉紫点缀 ===
val PinkAccent = Color(0xFFFF9EB5)
val PurpleAccent = Color(0xFFB5A8D5)

// === 旧颜色别名（兼容过渡） ===
val Pink = AccentOrange
val PinkDark = Color(0xFFCC6656)
val PinkLight = Color(0xFFFCE8E4)
val TextGray = LightOnSurfaceDim
val WarmWhite = LightBg
val ChatBubbleAssistant = DarkBubbleAssist
val ChatBubbleUser = DarkBubbleUser

// === 头像背景色板 ===
val AvatarColors = listOf(
    AccentOrange, AccentBlue, AccentGreen, AccentYellow,
    AccentPurple, AccentCyan, PinkAccent, Color(0xFF7EC8E3),
)

private val StDark = darkColorScheme(
    primary = AccentOrange, onPrimary = Color.White,
    primaryContainer = Color(0xFF4A2025), onPrimaryContainer = Color(0xFFFCD0D4),
    secondary = AccentBlue, onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A3A5C), onSecondaryContainer = Color(0xFFD0E8FF),
    tertiary = AccentGreen, onTertiary = Color.White,
    surface = DarkBg, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceDim,
    background = DarkBg, onBackground = DarkOnSurface,
    error = AccentOrange, onError = Color.White,
    outline = Color(0xFF3B3D54),
    outlineVariant = Color(0xFF2D2E42),
    inverseSurface = LightBg, inverseOnSurface = LightOnSurface,
)

private val StLight = lightColorScheme(
    primary = Color(0xFFCC6656), onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8E4), onPrimaryContainer = Color(0xFF4A2018),
    secondary = Color(0xFF4F7DB3), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4F0), onSecondaryContainer = Color(0xFF1A3A5C),
    tertiary = Color(0xFF5F8B5A), onTertiary = Color.White,
    surface = LightBg, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceDim,
    background = LightBg, onBackground = LightOnSurface,
    error = Color(0xFFCC6656), onError = Color.White,
    outline = Color(0xFFD0CDC8),
    outlineVariant = Color(0xFFE8E5E0),
    inverseSurface = DarkBg, inverseOnSurface = DarkOnSurface,
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
        colorScheme = if (isDark) StDark else StLight,
        content = content
    )
}
