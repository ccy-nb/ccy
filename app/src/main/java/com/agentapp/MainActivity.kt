package com.agentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agentapp.data.repository.SettingsRepository
import com.agentapp.ui.theme.AgentAppTheme
import com.agentapp.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository(this@MainActivity) }
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                settingsRepo.getThemeMode().collect { mode ->
                    themeMode = when (mode) {
                        "dark" -> ThemeMode.DARK
                        "light" -> ThemeMode.LIGHT
                        else -> ThemeMode.SYSTEM
                    }
                }
            }
            AgentAppTheme(themeMode = themeMode) {
                AgentAppNavHost()
            }
        }
    }
}
