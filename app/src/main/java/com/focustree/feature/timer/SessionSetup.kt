package com.focustree.feature.timer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focustree.core.time.Clock
import com.focustree.core.time.SystemClock
import java.util.UUID

const val MAX_INTENTION_LENGTH = 120
const val MIN_CUSTOM_DURATION_MINUTES = 5
const val MAX_CUSTOM_DURATION_MINUTES = 120
val PRESET_DURATIONS_MINUTES = listOf(15, 25, 45, 60)

data class SessionSetup(
    val intention: String,
    val durationMinutes: Int,
)

sealed interface SetupValidationError {
    data object IntentionTooLong : SetupValidationError

    data object DurationOutOfRange : SetupValidationError

    data object ActiveSessionExists : SetupValidationError
}

object SessionSetupValidator {
    fun validate(
        intention: String,
        durationMinutes: Int,
    ): Set<SetupValidationError> =
        buildSet {
            if (intention.length > MAX_INTENTION_LENGTH) add(SetupValidationError.IntentionTooLong)
            if (durationMinutes !in MIN_CUSTOM_DURATION_MINUTES..MAX_CUSTOM_DURATION_MINUTES) {
                add(SetupValidationError.DurationOutOfRange)
            }
        }
}

interface SessionGateway {
    fun hasActiveSession(): Boolean

    fun startSession(setup: SessionSetup)
}

class InMemorySessionGateway(
    clock: Clock = SystemClock,
) : SessionGateway {
    private val timer = TimerStateMachine(clock)

    override fun hasActiveSession(): Boolean = timer.session?.outcome == SessionOutcome.ACTIVE

    override fun startSession(setup: SessionSetup) {
        timer.start(UUID.randomUUID().toString(), setup)
    }
}

data class SetupUiState(
    val intention: String = "",
    val durationMinutes: Int = PRESET_DURATIONS_MINUTES[1],
    val customDurationText: String = "",
    val usesCustomDuration: Boolean = false,
    val errors: Set<SetupValidationError> = emptySet(),
    val started: Boolean = false,
)

sealed interface SetupEvent {
    data class IntentionChanged(
        val value: String,
    ) : SetupEvent

    data class PresetSelected(
        val minutes: Int,
    ) : SetupEvent

    data class CustomDurationChanged(
        val value: String,
    ) : SetupEvent

    data object StartClicked : SetupEvent
}

class SetupViewModel(
    private val gateway: SessionGateway,
) {
    var state by mutableStateOf(SetupUiState())
        private set

    fun onEvent(event: SetupEvent) {
        state =
            when (event) {
                is SetupEvent.IntentionChanged -> {
                    state.copy(intention = event.value, errors = emptySet())
                }

                is SetupEvent.PresetSelected -> {
                    state.copy(
                        durationMinutes = event.minutes,
                        usesCustomDuration = false,
                        customDurationText = "",
                        errors = emptySet(),
                    )
                }

                is SetupEvent.CustomDurationChanged -> {
                    state.copy(
                        customDurationText = event.value.filter(Char::isDigit),
                        usesCustomDuration = true,
                        errors = emptySet(),
                    )
                }

                SetupEvent.StartClicked -> {
                    start()
                }
            }
    }

    private fun start(): SetupUiState {
        val duration =
            if (state.usesCustomDuration) {
                state.customDurationText.toIntOrNull() ?: 0
            } else {
                state.durationMinutes
            }
        val errors = SessionSetupValidator.validate(state.intention, duration).toMutableSet()
        if (gateway.hasActiveSession()) errors += SetupValidationError.ActiveSessionExists
        if (errors.isNotEmpty()) return state.copy(errors = errors, started = false)
        gateway.startSession(SessionSetup(state.intention.trim(), duration))
        return state.copy(durationMinutes = duration, errors = emptySet(), started = true)
    }
}
