package com.focustree.feature.garden

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.focustree.core.preferences.FocusPreferences
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.core.preferences.ThemePreference
import com.focustree.core.ui.FocusTreeTheme
import com.focustree.feature.settings.SettingsContent
import com.focustree.feature.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun settingsShowsThemeDurationAndSyncOptInInLightTheme() {
        val repository = FakePreferencesRepository()
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val viewModel = remember { SettingsViewModel(repository, scope) }
            FocusTreeTheme(darkTheme = false) { SettingsContent(viewModel) }
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsDisplayed()
        composeRule.onNodeWithTag("default-duration").assertIsDisplayed()
        composeRule.onNodeWithText("Optional anonymous sync. Your sessions remain available offline.").assertIsDisplayed()
        composeRule.onNodeWithText("Sync is off. Your sessions stay on this device.").assertIsDisplayed()
    }

    @Test
    fun settingsRemainsAccessibleInDarkThemeAtLargeFontScale() {
        val repository = FakePreferencesRepository()
        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit
                        .Density(density = 1f, fontScale = 1.5f),
            ) {
                FocusTreeTheme(darkTheme = true) {
                    val scope = rememberCoroutineScope()
                    val viewModel = remember { SettingsViewModel(repository, scope) }
                    SettingsContent(viewModel)
                }
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("default-duration").assertExists()
        composeRule.onNodeWithTag("sync-opt-in").assertExists()
        composeRule.onNodeWithText("Sync is off. Your sessions stay on this device.").assertExists()
    }

    private class FakePreferencesRepository : FocusPreferencesRepository {
        override val preferences = MutableStateFlow(FocusPreferences())

        override suspend fun setTheme(theme: ThemePreference) {
            preferences.value = preferences.value.copy(theme = theme)
        }

        override suspend fun setDefaultDurationMinutes(minutes: Int) {
            preferences.value = preferences.value.copy(defaultDurationMinutes = minutes)
        }

        override suspend fun setSyncEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(syncEnabled = enabled)
        }
    }
}
