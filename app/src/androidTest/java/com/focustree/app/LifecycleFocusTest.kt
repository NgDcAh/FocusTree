package com.focustree.app

import android.Manifest
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.focustree.FocusTreeApplication
import com.focustree.MainActivity
import com.focustree.core.time.SystemClock
import com.focustree.feature.timer.FocusSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LifecycleFocusTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var app: FocusTreeApplication
    private lateinit var session: FocusSession

    @Before
    fun seedActiveSession() =
        runBlocking {
            app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FocusTreeApplication
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                "com.focustree",
                Manifest.permission.POST_NOTIFICATIONS,
            )
            app.sessions.activeSession()?.let { app.sessions.abandon(it.id, SystemClock.nowEpochMillis()) }
            val now = SystemClock.nowEpochMillis()
            session = FocusSession(UUID.randomUUID().toString(), "Persisted intention", 300_000L, now, now + 300_000L)
            app.sessions.insert(session)
        }

    @After
    fun clearActiveSession() =
        runBlocking {
            app.sessions.abandon(session.id, SystemClock.nowEpochMillis())
            Unit
        }

    @Test
    fun activeSessionRestoresAfterActivityRecreationWithoutDuplicateRecord() =
        runBlocking {
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Persisted intention").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Persisted intention").assertIsDisplayed()
            composeRule.activityRule.scenario.recreate()
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Persisted intention").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Persisted intention").assertIsDisplayed()

            assertEquals(session.id, app.sessions.activeSession()?.id)
            assertEquals(1, app.sessions.sessionHistory().count { it.id == session.id })
        }

    @Test
    fun activeSessionSurvivesLandscapeAndPortraitRotation() =
        runBlocking {
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Persisted intention").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Persisted intention").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            composeRule.waitUntil(5_000L) {
                composeRule.onAllNodesWithText("Persisted intention").fetchSemanticsNodes().isNotEmpty()
            }

            assertEquals(session.id, app.sessions.activeSession()?.id)
            assertEquals(1, app.sessions.sessionHistory().count { it.id == session.id })
            composeRule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            Unit
        }
}
