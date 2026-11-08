package com.focustree.feature.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSetupTest {
    @Test fun `presets are exactly supported`() = assertEquals(listOf(15, 25, 45, 60), PRESET_DURATIONS_MINUTES)

    @Test fun `custom duration validates boundaries`() {
        assertTrue(SessionSetupValidator.validate("", 5).isEmpty())
        assertTrue(SessionSetupValidator.validate("", 120).isEmpty())
        assertTrue(SetupValidationError.DurationOutOfRange in SessionSetupValidator.validate("", 4))
        assertTrue(SetupValidationError.DurationOutOfRange in SessionSetupValidator.validate("", 121))
    }

    @Test fun `intention is optional and limited`() {
        assertTrue(SessionSetupValidator.validate("", 25).isEmpty())
        assertTrue(SessionSetupValidator.validate("a".repeat(120), 25).isEmpty())
        assertTrue(SetupValidationError.IntentionTooLong in SessionSetupValidator.validate("a".repeat(121), 25))
    }

    @Test fun `second session is rejected`() {
        val gateway = RecordingGateway()
        val viewModel = SetupViewModel(gateway)
        viewModel.onEvent(SetupEvent.StartClicked)
        assertTrue(viewModel.state.started)
        viewModel.onEvent(SetupEvent.StartClicked)
        assertFalse(viewModel.state.started)
        assertTrue(SetupValidationError.ActiveSessionExists in viewModel.state.errors)
        assertEquals(1, gateway.started.size)
    }

    private class RecordingGateway : SessionGateway {
        val started = mutableListOf<SessionSetup>()

        override fun hasActiveSession(): Boolean = started.isNotEmpty()

        override fun startSession(setup: SessionSetup) {
            started += setup
        }
    }
}
