package com.focustree.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusSessionDaoTest {
    private lateinit var database: FocusTreeDatabase
    private lateinit var dao: FocusSessionDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusTreeDatabase::class.java).build()
        dao = database.focusSessionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun onlyOneActiveSessionCanBeStored() =
        runBlocking {
            dao.insert(session(id = "first", startedAt = 100L).toEntity())

            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking { dao.insert(session(id = "second", startedAt = 200L).toEntity()) }
            }
            Unit
        }

    @Test
    fun historyIsNewestFirstWithStableIdTieBreak() =
        runBlocking {
            dao.insert(session(id = "older", startedAt = 100L, outcome = SessionOutcome.ABANDONED).toEntity())
            dao.insert(session(id = "b", startedAt = 200L, outcome = SessionOutcome.COMPLETED).toEntity())
            dao.insert(session(id = "a", startedAt = 200L, outcome = SessionOutcome.COMPLETED).toEntity())

            assertEquals(listOf("b", "a", "older"), dao.sessionHistory().map { it.id })
        }

    @Test
    fun statisticsCountOnlyCompletedSessionsInRequestedRange() =
        runBlocking {
            dao.insert(
                session(
                    id = "included",
                    startedAt = 100L,
                    duration = 600L,
                    outcome = SessionOutcome.COMPLETED,
                    finishedAt = 700L,
                ).toEntity(),
            )
            dao.insert(
                session(
                    id = "abandoned",
                    startedAt = 200L,
                    duration = 900L,
                    outcome = SessionOutcome.ABANDONED,
                    finishedAt = 800L,
                ).toEntity(),
            )
            dao.insert(
                session(
                    id = "outside",
                    startedAt = 300L,
                    duration = 1_200L,
                    outcome = SessionOutcome.COMPLETED,
                    finishedAt = 1_500L,
                ).toEntity(),
            )

            assertEquals(600L, dao.completedDurationMillis(fromEpochMillis = 500L, untilEpochMillis = 1_000L))
        }

    @Test
    fun completionAndRewardAreAtomicAndIdempotent() =
        runBlocking {
            val store = RoomFocusSessionStore(dao, database.syncOutboxDao())
            store.insert(session(id = "active", startedAt = 100L))

            assertTrue(store.complete("active", finishedAtEpochMillis = 50L))
            assertFalse(store.complete("active", finishedAtEpochMillis = 900L))

            val stored = store.sessionHistory().single()
            assertEquals(SessionOutcome.COMPLETED, stored.outcome)
            assertEquals(100L, stored.finishedAtEpochMillis)
            assertTrue(stored.rewardGranted)
            assertNull(store.activeSession())
        }

    @Test
    fun completedAndAbandonedSessionsEnterOutboxOnlyOnce() =
        runBlocking {
            val store = RoomFocusSessionStore(dao, database.syncOutboxDao())
            store.insert(session(id = "completed", startedAt = 100L))
            assertTrue(store.complete("completed", 700L))
            assertFalse(store.complete("completed", 800L))

            store.insert(session(id = "abandoned", startedAt = 200L))
            assertTrue(store.abandon("abandoned", 900L))
            assertFalse(store.abandon("abandoned", 1_000L))

            assertEquals(listOf("completed", "abandoned"), store.pendingSyncSessions().map { it.id })
            assertTrue(store.sessionHistory().find { it.id == "completed" }!!.rewardGranted)
            assertFalse(store.sessionHistory().find { it.id == "abandoned" }!!.rewardGranted)
        }

    @Test
    fun importingTheSameRemoteSessionDoesNotDuplicateItsReward() =
        runBlocking {
            val store = RoomFocusSessionStore(dao, database.syncOutboxDao())
            val remote =
                session(
                    id = "remote",
                    startedAt = 100L,
                    outcome = SessionOutcome.COMPLETED,
                    finishedAt = 700L,
                )

            store.recordRemoteSessions(listOf(remote))
            store.recordRemoteSessions(listOf(remote))

            assertEquals(1, store.sessionHistory().count { it.id == "remote" })
            assertTrue(store.sessionHistory().single().rewardGranted)
            assertTrue(store.pendingSyncSessions().isEmpty())
        }

    private fun session(
        id: String,
        startedAt: Long,
        duration: Long = 600L,
        outcome: SessionOutcome = SessionOutcome.ACTIVE,
        finishedAt: Long? = null,
    ): FocusSession =
        FocusSession(
            id = id,
            intention = "Test",
            plannedDurationMillis = duration,
            startedAtEpochMillis = startedAt,
            expectedEndAtEpochMillis = startedAt + duration,
            finishedAtEpochMillis = finishedAt,
            outcome = outcome,
            rewardGranted = outcome == SessionOutcome.COMPLETED,
        )
}
