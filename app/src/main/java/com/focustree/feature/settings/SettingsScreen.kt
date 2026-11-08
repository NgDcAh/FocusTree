package com.focustree.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.core.preferences.ThemePreference

@Composable
fun SettingsScreen(
    repository: FocusPreferencesRepository,
    syncPreferenceAction: suspend (Boolean) -> Unit = repository::setSyncEnabled,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(repository) { SettingsViewModel(repository, scope, syncPreferenceAction) }
    SettingsContent(viewModel)
}

@Composable
fun SettingsContent(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ThemePreference.entries.forEach { theme ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.preferences.theme == theme,
                    onClick = { viewModel.setTheme(theme) },
                )
                Text(theme.name.lowercase().replaceFirstChar(Char::uppercase))
            }
        }
        OutlinedTextField(
            value = state.durationText,
            onValueChange = viewModel::setDefaultDuration,
            modifier = Modifier.fillMaxWidth().testTag("default-duration"),
            label = { Text("Default focus duration (minutes)") },
            supportingText = { Text(state.durationError ?: "Choose 5 to 120 minutes.") },
            isError = state.durationError != null,
        )
        Text("Optional sync", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sync completed sessions")
                Text(
                    "Optional anonymous sync. Your sessions remain available offline.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = state.preferences.syncEnabled,
                onCheckedChange = viewModel::setSyncEnabled,
                modifier =
                    Modifier.testTag("sync-opt-in").semantics {
                        contentDescription = "Enable optional session sync"
                    },
            )
        }
        Text(
            if (state.preferences.syncEnabled) {
                "Sync is on. Sessions upload when a network is available."
            } else {
                "Sync is off. Your sessions stay on this device."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        if (state.unavailable) {
            Text("Settings could not be saved. Try again.", color = MaterialTheme.colorScheme.error)
        }
    }
}
