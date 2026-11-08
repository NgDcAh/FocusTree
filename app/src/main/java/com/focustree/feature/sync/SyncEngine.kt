package com.focustree.feature.sync

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import java.io.IOException

/** Runs one idempotent synchronization pass. Room remains the local source of truth. */
class SyncEngine(
    private val api: SyncApi,
    private val credentials: SyncCredentialStore,
    private val sessions: SyncSessionRepository,
) {
    suspend fun runOnce() {
        val identity = credentials.read() ?: api.register().also(credentials::write)
        for (session in sessions.pendingSyncSessions()) {
            try {
                api.upload(identity, session)
                sessions.acknowledgeSync(session.id)
            } catch (error: IOException) {
                if (error is SyncHttpException) {
                    if (error.statusCode == HTTP_UNAUTHORIZED) credentials.clear()
                    sessions.recordSyncFailure(session.id, error.statusCode.toString())
                } else {
                    sessions.recordSyncFailure(session.id, "network")
                }
                throw error
            }
        }

        var beforeStartedAt: Long? = null
        var beforeId: String? = null
        do {
            val page = downloadPage(identity, beforeStartedAt, beforeId)
            sessions.recordRemoteSessions(page.sessions.map { it.toLocal() })
            beforeStartedAt = page.beforeStartedAt
            beforeId = page.beforeId
        } while (beforeStartedAt != null && beforeId != null)
    }

    private fun RemoteSession.toLocal() =
        FocusSession(
            id = id,
            intention = intention,
            plannedDurationMillis = plannedDurationMillis,
            startedAtEpochMillis = startedAtEpochMillis,
            expectedEndAtEpochMillis = startedAtEpochMillis + plannedDurationMillis,
            finishedAtEpochMillis = finishedAtEpochMillis,
            outcome = outcome,
            rewardGranted = outcome == SessionOutcome.COMPLETED,
        )

    private fun downloadPage(
        identity: SyncCredentials,
        beforeStartedAt: Long?,
        beforeId: String?,
    ): RemotePage =
        try {
            api.download(identity, beforeStartedAt, beforeId)
        } catch (error: SyncHttpException) {
            if (error.statusCode == HTTP_UNAUTHORIZED) credentials.clear()
            throw error
        }
}

interface SyncSessionRepository {
    suspend fun pendingSyncSessions(): List<FocusSession>

    suspend fun recordSyncFailure(
        sessionId: String,
        errorCode: String,
    )

    suspend fun acknowledgeSync(sessionId: String)

    suspend fun recordRemoteSessions(sessions: List<FocusSession>)
}

private const val HTTP_UNAUTHORIZED = 401
