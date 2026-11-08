package com.focustree.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.preferences.FocusPreferences
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.core.preferences.ThemePreference
import com.focustree.core.ui.FocusTreeTheme
import com.focustree.feature.garden.GardenScreen
import com.focustree.feature.garden.RoomGardenRepository
import com.focustree.feature.history.HistoryScreen
import com.focustree.feature.settings.SettingsScreen

private enum class HomeDestination {
    FOCUS,
    GARDEN,
    HISTORY,
    SETTINGS,
}

@Composable
fun FocusTreeApp(
    sessions: RoomFocusSessionStore,
    preferences: FocusPreferencesRepository,
    syncPreferenceAction: suspend (Boolean) -> Unit = preferences::setSyncEnabled,
) {
    val preferenceState by preferences.preferences.collectAsState(initial = FocusPreferences())
    val darkTheme =
        when (preferenceState.theme) {
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }
    FocusTreeTheme(darkTheme = darkTheme) {
        var destination by rememberSaveable { mutableStateOf(HomeDestination.FOCUS) }
        val gardenRepository = remember(sessions) { RoomGardenRepository(sessions) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = destination == HomeDestination.FOCUS,
                        onClick = { destination = HomeDestination.FOCUS },
                        icon = {},
                        label = { Text("Focus") },
                    )
                    NavigationBarItem(
                        selected = destination == HomeDestination.GARDEN,
                        onClick = { destination = HomeDestination.GARDEN },
                        icon = {},
                        label = { Text("Garden") },
                    )
                    NavigationBarItem(
                        selected = destination == HomeDestination.HISTORY,
                        onClick = { destination = HomeDestination.HISTORY },
                        icon = {},
                        label = { Text("History") },
                    )
                    NavigationBarItem(
                        selected = destination == HomeDestination.SETTINGS,
                        onClick = { destination = HomeDestination.SETTINGS },
                        icon = {},
                        label = { Text("Settings") },
                    )
                }
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                when (destination) {
                    HomeDestination.FOCUS -> LifecycleFocusScreen(sessions, preferenceState.defaultDurationMinutes)
                    HomeDestination.GARDEN -> GardenScreen(gardenRepository)
                    HomeDestination.HISTORY -> HistoryScreen(sessions)
                    HomeDestination.SETTINGS -> SettingsScreen(preferences, syncPreferenceAction)
                }
            }
        }
    }
}
