package com.focustree.core.database

import com.focustree.feature.timer.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFocusSessionStore(
    private val dao: FocusSessionDao,
    private val outboxDao: SyncOutboxDao,
) {
    suspend fun insert(session: FocusSession) = dao.insert(session.toEntity())

    suspend fun activeSession(): FocusSession? = dao.activeSession()?.toDomain()

    suspend fun sessionHistory(): List<FocusSession> = dao.sessionHistory().map(FocusSessionEntity::toDomain)

    fun observeSessionHistory(): Flow<List<FocusSession>> =
        dao.observeSessionHistory().map { sessions -> sessions.map(FocusSessionEntity::toDomain) }

    suspend fun completedDurationMillis(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): Long = dao.completedDurationMillis(fromEpochMillis, untilEpochMillis)

    suspend fun pendingSyncSessions(limit: Int = SYNC_BATCH_LIMIT): List<FocusSession> =
        outboxDao.pendingSyncSessions(limit).map(FocusSessionEntity::toDomain)

    suspend fun recordSyncFailure(
        sessionId: String,
        errorCode: String,
    ) = outboxDao.recordSyncFailure(sessionId, errorCode)

    suspend fun acknowledgeSync(sessionId: String) = outboxDao.acknowledgeSync(sessionId)

    suspend fun insertRemote(session: FocusSession): Boolean = dao.insertRemote(session.toEntity()) != -1L

    suspend fun complete(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Boolean = dao.completeActiveSessionAndQueueSync(sessionId, finishedAtEpochMillis) == 1

    suspend fun abandon(
        sessionId: String,
        finishedAtEpochMillis: Long,
    ): Boolean = dao.abandonActiveSessionAndQueueSync(sessionId, finishedAtEpochMillis) == 1
}

suspend fun RoomFocusSessionStore.recordRemoteSessions(sessions: List<FocusSession>) {
    sessions.forEach { insertRemote(it) }
}

private const val SYNC_BATCH_LIMIT = 50
