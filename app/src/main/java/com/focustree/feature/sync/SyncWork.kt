package com.focustree.feature.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focustree.BuildConfig
import com.focustree.FocusTreeApplication
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.database.recordRemoteSessions
import com.focustree.feature.timer.FocusSession
import java.util.concurrent.TimeUnit

class SyncScheduler(
    context: Context,
) {
    private val work = WorkManager.getInstance(context.applicationContext)

    fun enable() {
        val request =
            PeriodicWorkRequestBuilder<SessionSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun disable() {
        work.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "focus-session-sync"
    }
}

class SessionSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as FocusTreeApplication
        val outcome =
            executeSyncWork(app.preferences.isSyncEnabled(), BuildConfig.SYNC_API_URL) {
                SyncEngine(
                    HttpsSyncApi(BuildConfig.SYNC_API_URL),
                    app.syncCredentials,
                    RoomSyncSessionRepository(app.sessions),
                ).runOnce()
            }
        return when (outcome) {
            SyncWorkOutcome.SUCCESS -> Result.success()
            SyncWorkOutcome.RETRY -> Result.retry()
            SyncWorkOutcome.FAILURE -> Result.failure()
        }
    }
}

class RoomSyncSessionRepository(
    private val sessions: RoomFocusSessionStore,
) : SyncSessionRepository {
    override suspend fun pendingSyncSessions() = sessions.pendingSyncSessions()

    override suspend fun recordSyncFailure(
        sessionId: String,
        errorCode: String,
    ) = sessions.recordSyncFailure(sessionId, errorCode)

    override suspend fun acknowledgeSync(sessionId: String) = sessions.acknowledgeSync(sessionId)

    override suspend fun recordRemoteSessions(sessions: List<FocusSession>) {
        this.sessions.recordRemoteSessions(sessions)
    }
}
