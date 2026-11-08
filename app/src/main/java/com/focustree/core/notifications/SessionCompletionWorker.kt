package com.focustree.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focustree.FocusTreeApplication
import com.focustree.core.time.SystemClock

class SessionCompletionWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
        val app = applicationContext as FocusTreeApplication
        val session = sessionId?.let { id -> app.sessions.activeSession()?.takeIf { it.id == id } }
        return when {
            sessionId == null -> {
                Result.failure()
            }

            session == null -> {
                Result.success()
            }

            session.expectedEndAtEpochMillis > SystemClock.nowEpochMillis() -> {
                Result.retry()
            }

            else -> {
                if (app.sessions.complete(sessionId, SystemClock.nowEpochMillis())) {
                    CompletionNotifications.show(applicationContext, sessionId, session.intention)
                }
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
    }
}
