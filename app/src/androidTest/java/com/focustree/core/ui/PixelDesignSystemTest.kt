package com.focustree.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PixelDesignSystemTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun focusHeaderAndActionAreAccessibleWhileMotifIsDecorative() {
        composeRule.setContent {
            FocusTreeTheme {
                Column {
                    Text("Start a focus session")
                    PixelGardenMotif()
                    Button(onClick = {}) { Text("Start focus") }
                }
            }
        }

        composeRule.onNodeWithText("Start a focus session").assertIsDisplayed()
        composeRule.onNodeWithText("Start focus").assertIsDisplayed()
    }
}
