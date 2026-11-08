package com.focustree.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemote(session: FocusSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutbox(entry: SyncOutboxEntity)

    @Query("SELECT * FROM focus_sessions WHERE outcome = 'ACTIVE' LIMIT 1")
    suspend fun activeSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY started_at_epoch_millis DESC, id DESC")
    suspend fun sessionHistory(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY started_at_epoch_millis DESC, id DESC")
    fun observeSessionHistory(): Flow<List<FocusSessionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(planned_duration_millis), 0)
        FROM focus_sessions
        WHERE outcome = 'COMPLETED'
          AND finished_at_epoch_millis >= :fromEpochMillis
          AND finished_at_epoch_millis < :untilEpochMillis
        """,
    )
    suspend fun completedDurationMillis(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): Long

    @Query(
        """
        UPDATE focus_sessions
        SET outcome = 'COMPLETED',
            finished_at_epoch_millis = MAX(:finishedAtEpochMillis, started_at_epoch_millis),
            reward_granted = 1,
            active_slot = NULL
        WHERE id = :sessionId AND outcome = 'ACTIVE'
        """,
    )
    suspend fun completeActiveSession(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Int

    @Transaction
    suspend fun completeActiveSessionAndQueueSync(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Int {
        val updated = completeActiveSession(sessionId, finishedAtEpochMillis)
        if (updated == 1) insertOutbox(SyncOutboxEntity(sessionId, finishedAtEpochMillis))
        return updated
    }

    @Query(
        """
        UPDATE focus_sessions
        SET outcome = 'ABANDONED',
            finished_at_epoch_millis = MAX(:finishedAtEpochMillis, started_at_epoch_millis),
            reward_granted = 0,
            active_slot = NULL
        WHERE id = :sessionId AND outcome = 'ACTIVE'
        """,
    )
    suspend fun abandonActiveSession(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Int

    @Transaction
    suspend fun abandonActiveSessionAndQueueSync(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Int {
        val updated = abandonActiveSession(sessionId, finishedAtEpochMillis)
        if (updated == 1) insertOutbox(SyncOutboxEntity(sessionId, finishedAtEpochMillis))
        return updated
    }
}
