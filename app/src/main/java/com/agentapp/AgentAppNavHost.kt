package com.agentapp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentapp.viewmodel.ChatListViewModel
import com.agentapp.viewmodel.CharacterListViewModel
import com.agentapp.viewmodel.SettingsViewModel
import com.agentapp.viewmodel.WorldBookViewModel

@Composable
fun AgentAppNavHost() {
    val characterListViewModel: CharacterListViewModel = viewModel()
    val chatListViewModel: ChatListViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val worldBookViewModel: WorldBookViewModel = viewModel()

    MainScreen(
        characterListViewModel = characterListViewModel,
        chatListViewModel = chatListViewModel,
        settingsViewModel = settingsViewModel,
        worldBookViewModel = worldBookViewModel
    )
}
