package com.focustree.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.focustree.feature.timer.MAX_CUSTOM_DURATION_MINUTES
import com.focustree.feature.timer.MIN_CUSTOM_DURATION_MINUTES
import com.focustree.feature.timer.SessionSetupValidator
import com.focustree.feature.timer.SetupValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DEFAULT_DURATION_MINUTES = 25

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

data class FocusPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val defaultDurationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val syncEnabled: Boolean = false,
)

interface FocusPreferencesRepository {
    val preferences: Flow<FocusPreferences>

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setDefaultDurationMinutes(minutes: Int)

    suspend fun setSyncEnabled(enabled: Boolean)

    suspend fun isSyncEnabled(): Boolean = preferences.first().syncEnabled
}

class DataStoreFocusPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : FocusPreferencesRepository {
    override val preferences: Flow<FocusPreferences> =
        dataStore.data.map { stored ->
            FocusPreferences(
                theme = stored[THEME_KEY]?.toThemePreference() ?: ThemePreference.SYSTEM,
                defaultDurationMinutes =
                    stored[DEFAULT_DURATION_KEY]
                        ?.takeIf { it in MIN_CUSTOM_DURATION_MINUTES..MAX_CUSTOM_DURATION_MINUTES }
                        ?: DEFAULT_DURATION_MINUTES,
                syncEnabled = stored[SYNC_ENABLED_KEY] ?: false,
            )
        }

    override suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    override suspend fun setDefaultDurationMinutes(minutes: Int) {
        require(
            SessionSetupValidator.validate("", minutes).none { it == SetupValidationError.DurationOutOfRange },
        ) {
            "Default duration must be between $MIN_CUSTOM_DURATION_MINUTES and $MAX_CUSTOM_DURATION_MINUTES minutes"
        }
        dataStore.edit { it[DEFAULT_DURATION_KEY] = minutes }
    }

    override suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SYNC_ENABLED_KEY] = enabled }
    }

    private fun String.toThemePreference(): ThemePreference? = ThemePreference.entries.find { it.name == this }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DEFAULT_DURATION_KEY = intPreferencesKey("default_duration_minutes")
        val SYNC_ENABLED_KEY =
            androidx.datastore.preferences.core
                .booleanPreferencesKey("sync_enabled")
    }
}
