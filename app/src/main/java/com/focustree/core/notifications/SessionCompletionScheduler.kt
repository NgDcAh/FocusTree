package com.focustree.core.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SessionCompletionScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(
        sessionId: String,
        expectedEndAtEpochMillis: Long,
        nowEpochMillis: Long,
    ) {
        val request =
            OneTimeWorkRequestBuilder<SessionCompletionWorker>()
                .setInitialDelay((expectedEndAtEpochMillis - nowEpochMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(SessionCompletionWorker.KEY_SESSION_ID, sessionId).build())
                .build()
        workManager.enqueueUniqueWork(workName(sessionId), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(sessionId: String) {
        workManager.cancelUniqueWork(workName(sessionId))
    }

    private fun workName(sessionId: String) = "complete-focus-$sessionId"
}
