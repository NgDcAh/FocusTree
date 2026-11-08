package com.focustree.feature.settings

import com.focustree.core.preferences.FocusPreferences
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.core.preferences.ThemePreference
import com.focustree.feature.timer.SessionSetupValidator
import com.focustree.feature.timer.SetupValidationError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: FocusPreferences = FocusPreferences(),
    val durationText: String = FocusPreferences().defaultDurationMinutes.toString(),
    val durationError: String? = null,
    val unavailable: Boolean = false,
)

class SettingsViewModel(
    private val repository: FocusPreferencesRepository,
    private val scope: CoroutineScope,
    private val syncPreferenceAction: suspend (Boolean) -> Unit = repository::setSyncEnabled,
) {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            repository.preferences
                .catch { mutableState.value = mutableState.value.copy(unavailable = true) }
                .collect {
                    mutableState.value =
                        mutableState.value.copy(
                            preferences = it,
                            durationText = it.defaultDurationMinutes.toString(),
                            unavailable = false,
                        )
                }
        }
    }

    fun setTheme(theme: ThemePreference) {
        update { repository.setTheme(theme) }
    }

    fun setDefaultDuration(text: String) {
        mutableState.value = mutableState.value.copy(durationText = text, durationError = null)
        val duration = text.toIntOrNull()
        if (
            duration == null ||
            SetupValidationError.DurationOutOfRange in SessionSetupValidator.validate("", duration)
        ) {
            mutableState.value = mutableState.value.copy(durationError = "Choose a duration from 5 to 120 minutes.")
            return
        }
        update { repository.setDefaultDurationMinutes(duration) }
    }

    fun setSyncEnabled(enabled: Boolean) {
        update { syncPreferenceAction(enabled) }
    }

    private fun update(action: suspend () -> Unit) {
        // UI edits are best-effort; repository state remains the source of truth.
        scope.launch {
            runCatching { action() }
                .onFailure { mutableState.value = mutableState.value.copy(unavailable = true) }
        }
    }
}
