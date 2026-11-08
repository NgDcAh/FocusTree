package com.focustree.app

import android.Manifest
import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.focustree.FocusTreeApplication
import com.focustree.core.notifications.CompletionNotifications
import com.focustree.core.notifications.SessionCompletionScheduler
import com.focustree.core.time.SystemClock
import com.focustree.feature.timer.FocusSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackgroundCompletionTest {
    @Test
    fun backgroundWorkerCompletesOnceAndPostsNotification() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val app = context.applicationContext as FocusTreeApplication
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            CompletionNotifications.createChannel(context)
            app.sessions.activeSession()?.let { app.sessions.abandon(it.id, SystemClock.nowEpochMillis()) }
            val now = SystemClock.nowEpochMillis()
            val id = UUID.randomUUID().toString()
            val session = FocusSession(id, "Background test", 5_000L, now, now + 5_000L)
            app.sessions.insert(session)
            SessionCompletionScheduler(context).schedule(id, session.expectedEndAtEpochMillis, now)
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()

            var completed = false
            for (attempt in 0 until 45) {
                delay(1_000L)
                val stored = app.sessions.sessionHistory().single { it.id == id }
                if (stored.rewardGranted) {
                    completed = true
                    break
                }
            }

            val stored = app.sessions.sessionHistory().single { it.id == id }
            assertTrue("Worker must finalize the session while the app is backgrounded", completed)
            assertEquals(1, app.sessions.sessionHistory().count { it.id == id })
            assertTrue(stored.rewardGranted)
            assertTrue(
                context
                    .getSystemService(NotificationManager::class.java)
                    .activeNotifications
                    .any { it.id == id.hashCode() },
            )
        }
}
