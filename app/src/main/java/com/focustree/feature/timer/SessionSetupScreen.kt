package com.focustree.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SessionSetupScreen(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Start a focus session", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = state.intention,
                onValueChange = { onEvent(SetupEvent.IntentionChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Intention (optional)") },
                supportingText = { Text("${state.intention.length}/$MAX_INTENTION_LENGTH") },
                isError = SetupValidationError.IntentionTooLong in state.errors,
            )
            Text("Duration", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_DURATIONS_MINUTES.chunked(2).forEach { rowDurations ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowDurations.forEach { minutes ->
                            FilterChip(
                                selected = !state.usesCustomDuration && state.durationMinutes == minutes,
                                onClick = { onEvent(SetupEvent.PresetSelected(minutes)) },
                                modifier = Modifier.weight(1f),
                                label = { Text("$minutes min") },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = state.customDurationText,
                onValueChange = { onEvent(SetupEvent.CustomDurationChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Custom minutes (5–120)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = SetupValidationError.DurationOutOfRange in state.errors,
            )
            state.errors.forEach { error ->
                Text(
                    text =
                        when (error) {
                            SetupValidationError.IntentionTooLong -> "Intention must be 120 characters or fewer."
                            SetupValidationError.DurationOutOfRange -> "Choose a duration from 5 to 120 minutes."
                            SetupValidationError.ActiveSessionExists -> "Finish or cancel the active session first."
                        },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = { onEvent(SetupEvent.StartClicked) }) { Text("Start focus") }
            if (state.started) Text("Focus session started for ${state.durationMinutes} minutes.")
        }
    }
}
