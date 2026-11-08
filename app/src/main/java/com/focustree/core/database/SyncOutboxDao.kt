package com.focustree.core.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Query(
        """
        SELECT focus_sessions.* FROM focus_sessions
        INNER JOIN sync_outbox ON sync_outbox.session_id = focus_sessions.id
        ORDER BY sync_outbox.queued_at_epoch_millis ASC, focus_sessions.id ASC
        LIMIT :limit
        """,
    )
    suspend fun pendingSyncSessions(limit: Int): List<FocusSessionEntity>

    @Query(
        """
        UPDATE sync_outbox
        SET attempt_count = attempt_count + 1, last_error_code = :errorCode
        WHERE session_id = :sessionId
        """,
    )
    suspend fun recordSyncFailure(
        sessionId: String,
        errorCode: String,
    )

    @Query("DELETE FROM sync_outbox WHERE session_id = :sessionId")
    suspend fun acknowledgeSync(sessionId: String)
}
