package com.focustree.feature.timer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class SessionSetupScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun customDurationShowsErrorThenStartsWhenValid() {
        val viewModel = SetupViewModel(FakeGateway())
        composeRule.setContent { SessionSetupScreen(viewModel.state, viewModel::onEvent) }
        composeRule.onNodeWithText("Custom minutes (5–120)").performTextInput("4")
        composeRule.onNodeWithText("Start focus").performClick()
        composeRule.onNodeWithText("Choose a duration from 5 to 120 minutes.").assertIsDisplayed()
        composeRule.onNodeWithText("Custom minutes (5–120)").performTextInput("5")
        composeRule.onNodeWithText("Start focus").performClick()
        composeRule.onNodeWithText("Focus session started for 45 minutes.").assertIsDisplayed()
    }

    private class FakeGateway : SessionGateway {
        override fun hasActiveSession(): Boolean = false

        override fun startSession(setup: SessionSetup) = Unit
    }
}
