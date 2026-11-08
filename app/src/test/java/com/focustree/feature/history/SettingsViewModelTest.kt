package com.focustree.feature.history

import com.focustree.core.preferences.FocusPreferences
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.core.preferences.ThemePreference
import com.focustree.feature.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun `duration uses setup validation and valid changes plus theme and sync persist`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = FakePreferencesRepository()
            val viewModel = SettingsViewModel(repository, scope)
            viewModel.setDefaultDuration("4")
            assertNotNull(viewModel.state.value.durationError)
            assertEquals(25, repository.preferences.value.defaultDurationMinutes)

            viewModel.setDefaultDuration("45")
            viewModel.setTheme(ThemePreference.DARK)
            viewModel.setSyncEnabled(true)
            assertEquals(45, repository.preferences.value.defaultDurationMinutes)
            assertEquals(ThemePreference.DARK, repository.preferences.value.theme)
            assertEquals(true, repository.preferences.value.syncEnabled)
        } finally {
            scope.cancel()
        }
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
