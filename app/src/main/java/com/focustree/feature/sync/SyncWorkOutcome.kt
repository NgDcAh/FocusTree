package com.focustree.feature.sync

import android.database.sqlite.SQLiteException
import org.json.JSONException
import java.io.IOException
import java.security.GeneralSecurityException

enum class SyncWorkOutcome {
    SUCCESS,
    RETRY,
    FAILURE,
}

internal suspend fun executeSyncWork(
    syncEnabled: Boolean,
    apiUrl: String,
    sync: suspend () -> Unit,
): SyncWorkOutcome =
    when {
        !syncEnabled -> {
            SyncWorkOutcome.SUCCESS
        }

        apiUrl.isBlank() -> {
            SyncWorkOutcome.RETRY
        }

        else -> {
            try {
                sync()
                SyncWorkOutcome.SUCCESS
            } catch (_: IllegalArgumentException) {
                SyncWorkOutcome.FAILURE
            } catch (_: IOException) {
                SyncWorkOutcome.RETRY
            } catch (_: GeneralSecurityException) {
                SyncWorkOutcome.RETRY
            } catch (_: JSONException) {
                SyncWorkOutcome.RETRY
            } catch (_: SQLiteException) {
                SyncWorkOutcome.RETRY
            }
        }
    }
