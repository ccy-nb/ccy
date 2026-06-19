package com.agentapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════
// 纯黑风格 — 黑底 + 白字，极简
// ═══════════════════════════════════════════

// === 深色模式 ===
val DarkBg = Color(0xFF000000)           // 纯黑底
val DarkSurface = Color(0xFF111111)       // 黑表面
val DarkSurfaceVariant = Color(0xFF1A1A1A) // 变体
val DarkOnSurface = Color(0xFFE8E8E8)    // 白文字
val DarkOnSurfaceDim = Color(0xFF888888) // 灰色文字
val DarkBubbleAssist = Color(0xFF1A1A1A) // AI 气泡
val DarkBubbleUser = Color(0xFF222222)   // 用户气泡

// === 浅色模式 ===
val LightBg = Color(0xFFFFFFFF)          // 纯白底
val LightSurface = Color(0xFFFFFFFF)     // 白底
val LightSurfaceVariant = Color(0xFFF0F0F0) // 浅灰
val LightOnSurface = Color(0xFF1A1A1A)  // 黑文字
val LightOnSurfaceDim = Color(0xFF888888) // 灰色文字
val LightBubbleAssist = Color(0xFFF0F0F0) // AI 气泡
val LightBubbleUser = Color(0xFFE3E3E3)  // 用户气泡

// === 极简配色（单色系） ===
val TavPurple = Color(0xFF666666)        // 中性灰 accent
val TavPurpleLight = Color(0xFF999999)   // 浅灰
val TavPurpleDark = Color(0xFF333333)    // 深灰
val TavOrange = Color(0xFF888888)        // 灰
val TavGreen = Color(0xFF559955)         // 暗绿
val TavRed = Color(0xFFCC4444)           // 暗红
val TavBlue = Color(0xFF5588CC)          // 暗蓝

// === 头像背景色板 ===
val AvatarColors = listOf(
    TavPurple, TavOrange, TavGreen, TavBlue,
    Color(0xFF9D38BD), Color(0xFF3E8B8A), Color(0xFFDF0030), Color(0xFFFFA500),
)

// === 旧颜色别名（兼容过渡） ===
val AccentOrange = TavPurple
val AccentBlue = TavBlue
val AccentGreen = TavGreen
val AccentYellow = TavOrange
val AccentPurple = TavPurple
val AccentCyan = Color(0xFF06B6D4)
val Pink = TavPurpleDark
val PinkDark = Color(0xFF884444)
val PinkLight = Color(0xFFE8E8E8)

// === 额外别名（兼容之前 Material3 配色导入） ===
val CoralAccent = TavRed
val TealAccent = TavGreen
val IndigoLight = TavPurpleLight
val IndigoPrimary = TavPurple
val TextGray = LightOnSurfaceDim
val WarmWhite = LightBg
val ChatBubbleAssistant = DarkBubbleAssist
val ChatBubbleUser = DarkBubbleUser

private val M3Dark = darkColorScheme(
    primary = Color(0xFF666666), onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333), onPrimaryContainer = Color(0xFFCCCCCC),
    secondary = Color(0xFF555555), onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2A2A), onSecondaryContainer = Color(0xFFAAAAAA),
    tertiary = Color(0xFF5588CC), onTertiary = Color.Black,
    surface = DarkBg, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceDim,
    background = DarkBg, onBackground = DarkOnSurface,
    error = TavRed, onError = Color.White,
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF222222),
    inverseSurface = LightBg, inverseOnSurface = LightOnSurface,
)

private val M3Light = lightColorScheme(
    primary = Color(0xFF555555), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8E8), onPrimaryContainer = Color(0xFF222222),
    secondary = Color(0xFF777777), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0), onSecondaryContainer = Color(0xFF333333),
    tertiary = Color(0xFF5588CC), onTertiary = Color.White,
    surface = LightBg, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceDim,
    background = LightBg, onBackground = LightOnSurface,
    error = Color(0xFFCC4444), onError = Color.White,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
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
        colorScheme = if (isDark) M3Dark else M3Light,
        content = content
    )
}
