package com.focustree.feature.garden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.focustree.core.ui.FocusTreeTheme
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GardenScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun completedSessionRendersOneAccessiblePlantAndAbandonedSessionRendersNone() {
        val window = GardenDayWindow(1_000L, 2_000L)
        val growth =
            GardenProjection.projectToday(
                listOf(
                    session("complete", "Read notes", SessionOutcome.COMPLETED, 1_500L),
                    session("cancelled", "Cancelled task", SessionOutcome.ABANDONED, 1_600L),
                ),
                window,
            )
        composeRule.setContent {
            FocusTreeTheme {
                GardenScreenContent(GardenUiState(isLoading = false, growthItems = growth))
            }
        }

        assertEquals(1, composeRule.onAllNodesWithTag(GARDEN_GROWTH_ITEM_TAG).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Plant 1").assertIsDisplayed()
        composeRule.onNodeWithText("Read notes").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Cancelled task").fetchSemanticsNodes().size)
    }

    @Test
    fun emptyGardenExplainsHowToGrowTheFirstPlant() {
        composeRule.setContent {
            FocusTreeTheme {
                GardenScreenContent(GardenUiState(isLoading = false))
            }
        }

        composeRule.onNodeWithText("No plants yet today").assertIsDisplayed()
        composeRule.onNodeWithText("Complete a focus session to grow one.").assertIsDisplayed()
    }

    private fun session(
        id: String,
        intention: String,
        outcome: SessionOutcome,
        finishedAt: Long,
    ) = FocusSession(
        id = id,
        intention = intention,
        plannedDurationMillis = 600L,
        startedAtEpochMillis = finishedAt - 600L,
        expectedEndAtEpochMillis = finishedAt,
        finishedAtEpochMillis = finishedAt,
        outcome = outcome,
        rewardGranted = outcome == SessionOutcome.COMPLETED,
    )
}
