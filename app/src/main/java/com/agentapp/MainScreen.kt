package com.agentapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.ui.screens.ChatListScreen
import com.agentapp.ui.screens.CharacterListScreen
import com.agentapp.ui.screens.SettingsScreen
import com.agentapp.ui.screens.WorldBookScreen
import com.agentapp.ui.theme.AccentOrange
import com.agentapp.viewmodel.ChatListViewModel
import com.agentapp.viewmodel.CharacterListViewModel
import com.agentapp.viewmodel.SettingsViewModel
import com.agentapp.viewmodel.WorldBookViewModel

@Composable
fun MainScreen(
    characterListViewModel: CharacterListViewModel,
    chatListViewModel: ChatListViewModel,
    settingsViewModel: SettingsViewModel,
    worldBookViewModel: WorldBookViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("角色", "对话", "世界书", "设置")
    val tabEmojis = listOf("🌸", "💬", "📖", "⚙️")
    val tabIcons = listOf(Icons.Default.Person, Icons.AutoMirrored.Filled.Chat, null, Icons.Default.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tabIcons[index] != null) {
                                Icon(tabIcons[index]!!, contentDescription = label)
                            } else {
                                Text(tabEmojis[index], fontSize = 20.sp)
                            }
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentOrange,
                            selectedTextColor = AccentOrange,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> CharacterListScreen(
                modifier = Modifier.padding(padding),
                characterListViewModel = characterListViewModel
            )
            1 -> ChatListScreen(
                modifier = Modifier.padding(padding),
                chatListViewModel = chatListViewModel,
                characterListViewModel = characterListViewModel
            )
            2 -> WorldBookScreen(
                worldBookViewModel = worldBookViewModel,
                onBack = { },
                showBack = false
            )
            3 -> SettingsScreen(
                modifier = Modifier.padding(padding),
                settingsViewModel = settingsViewModel,
                onNavigateToWorldBook = { selectedTab = 2 }
            )
        }
    }
}
