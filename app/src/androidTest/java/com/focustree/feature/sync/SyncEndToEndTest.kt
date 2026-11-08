package com.focustree.feature.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.focustree.core.database.FocusTreeDatabase
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SyncEndToEndTest {
    @Test
    fun postgresSyncRepeatsIdempotentlyAndOptOutClearsCredential() {
        runBlocking {
            val apiUrl = com.focustree.BuildConfig.SYNC_API_URL
            assumeFalse(
                "Provide -PfocusTreeSyncApiUrl=https://localhost:8443",
                apiUrl.isBlank(),
            )
            val context = ApplicationProvider.getApplicationContext<Context>()
            val app = context.applicationContext as com.focustree.FocusTreeApplication
            val database = Room.inMemoryDatabaseBuilder(context, FocusTreeDatabase::class.java).build()
            val sessions = RoomFocusSessionStore(database.focusSessionDao(), database.syncOutboxDao())
            val api = HttpsSyncApi(apiUrl)
            app.syncScheduler.disable()
            app.syncCredentials.clear()
            app.preferences.setSyncEnabled(false)
            try {
                val identity = api.register()
                app.syncCredentials.write(identity)
                val id = UUID.randomUUID().toString()
                val startedAt = System.currentTimeMillis()
                sessions.insert(
                    FocusSession(
                        id = id,
                        intention = "Local sync E2E",
                        plannedDurationMillis = TEST_DURATION_MILLIS,
                        startedAtEpochMillis = startedAt,
                        expectedEndAtEpochMillis = startedAt + TEST_DURATION_MILLIS,
                    ),
                )
                assertTrue(sessions.complete(id, startedAt + TEST_DURATION_MILLIS))

                val engine = SyncEngine(api, app.syncCredentials, RoomSyncSessionRepository(sessions))
                engine.runOnce()
                engine.runOnce()

                assertTrue(sessions.pendingSyncSessions().none { it.id == id })
                val localCopy = sessions.sessionHistory().filter { it.id == id }
                assertEquals(1, localCopy.size)
                assertEquals(SessionOutcome.COMPLETED, localCopy.single().outcome)
                assertTrue(localCopy.single().rewardGranted)

                app.setSyncEnabled(true)
                val scheduled =
                    WorkManager
                        .getInstance(context)
                        .getWorkInfosForUniqueWork(SyncScheduler.WORK_NAME)
                        .get(WORK_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                assertTrue(
                    "Expected unique sync work to be enqueued, got ${scheduled.map { it.state }}",
                    scheduled.any { it.state == WorkInfo.State.ENQUEUED },
                )
                app.setSyncEnabled(false)
                val remaining =
                    WorkManager
                        .getInstance(context)
                        .getWorkInfosForUniqueWork(SyncScheduler.WORK_NAME)
                        .get(WORK_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                assertFalse(remaining.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING })
                val syncEnabled = app.preferences.isSyncEnabled()
                assertFalse(syncEnabled)
                assertNull(app.syncCredentials.read())
            } finally {
                if (app.preferences.isSyncEnabled()) app.setSyncEnabled(false)
                database.close()
            }
        }
    }

    private companion object {
        const val TEST_DURATION_MILLIS = 300_000L
        const val WORK_QUERY_TIMEOUT_SECONDS = 5L
    }
}
