package com.focustree.feature.garden

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focustree.core.database.FocusTreeDatabase
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.time.Clock
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class RoomGardenRepositoryTest {
    private lateinit var database: FocusTreeDatabase
    private lateinit var sessions: RoomFocusSessionStore

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusTreeDatabase::class.java).build()
        sessions = RoomFocusSessionStore(database.focusSessionDao(), database.syncOutboxDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun gardenObservesOnlyPersistedCompletedSessionsFromToday() =
        runBlocking {
            val zone = TimeZone.getTimeZone("UTC")
            val todayStart =
                Calendar
                    .getInstance(zone)
                    .apply {
                        timeInMillis = FIXED_NOW
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
            sessions.insert(session("today", SessionOutcome.COMPLETED, todayStart + 1_000L))
            sessions.insert(session("abandoned", SessionOutcome.ABANDONED, todayStart + 2_000L))
            sessions.insert(session("yesterday", SessionOutcome.COMPLETED, todayStart - 1L))
            val repository =
                RoomGardenRepository(
                    sessions,
                    Clock { FIXED_NOW },
                    { zone },
                )

            val growth = repository.observeTodayGarden().first()

            assertEquals(listOf("today"), growth.map(GardenGrowth::sessionId))
        }

    private fun session(
        id: String,
        outcome: SessionOutcome,
        finishedAt: Long,
    ) = FocusSession(
        id = id,
        intention = id,
        plannedDurationMillis = DURATION_MILLIS,
        startedAtEpochMillis = finishedAt - DURATION_MILLIS,
        expectedEndAtEpochMillis = finishedAt,
        finishedAtEpochMillis = finishedAt,
        outcome = outcome,
        rewardGranted = outcome == SessionOutcome.COMPLETED,
    )

    private companion object {
        const val FIXED_NOW = 1_794_000_000_000L
        const val DURATION_MILLIS = 600L
    }
}
