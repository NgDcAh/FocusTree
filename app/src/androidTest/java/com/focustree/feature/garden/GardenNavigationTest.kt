package com.focustree.feature.garden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.focustree.FocusTreeApplication
import com.focustree.MainActivity
import com.focustree.core.time.SystemClock
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GardenNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var app: FocusTreeApplication
    private lateinit var completedId: String

    @Before
    fun seedSessions() =
        runBlocking {
            app =
                InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FocusTreeApplication
            val now = SystemClock.nowEpochMillis()
            completedId = UUID.randomUUID().toString()
            app.sessions.insert(
                session(completedId, "Garden completed intention", SessionOutcome.COMPLETED, now),
            )
            app.sessions.insert(
                session(UUID.randomUUID().toString(), "Garden abandoned intention", SessionOutcome.ABANDONED, now),
            )
        }

    @Test
    fun gardenNavigationShowsPersistedCompletionAndOmitsAbandonment() =
        runBlocking {
            composeRule.onNodeWithText("Garden").performClick()
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Garden completed intention").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithText("Garden completed intention").assertIsDisplayed()
            assertEquals(
                0,
                composeRule.onAllNodesWithText("Garden abandoned intention").fetchSemanticsNodes().size,
            )
            assertEquals(1, app.sessions.sessionHistory().count { it.id == completedId })
        }

    private fun session(
        id: String,
        intention: String,
        outcome: SessionOutcome,
        now: Long,
    ): FocusSession =
        FocusSession(
            id = id,
            intention = intention,
            plannedDurationMillis = PLANNED_DURATION_MILLIS,
            startedAtEpochMillis = now - PLANNED_DURATION_MILLIS,
            expectedEndAtEpochMillis = now,
            finishedAtEpochMillis = now,
            outcome = outcome,
            rewardGranted = outcome == SessionOutcome.COMPLETED,
        )

    private companion object {
        const val PLANNED_DURATION_MILLIS = 900_000L
    }
}
