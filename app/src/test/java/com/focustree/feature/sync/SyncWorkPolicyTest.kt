package com.focustree.feature.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncWorkPolicyTest {
    @Test
    fun disabledSyncDoesNotRunNetworkWork() =
        runBlocking {
            var ran = false

            val outcome = executeSyncWork(syncEnabled = false, apiUrl = API_URL) { ran = true }

            assertEquals(SyncWorkOutcome.SUCCESS, outcome)
            assertFalse(ran)
        }

    @Test
    fun missingApiConfigurationRetriesWithoutNetworkWork() =
        runBlocking {
            var ran = false

            val outcome = executeSyncWork(syncEnabled = true, apiUrl = " ") { ran = true }

            assertEquals(SyncWorkOutcome.RETRY, outcome)
            assertFalse(ran)
        }

    @Test
    fun offlineAndServerFailuresAreRetried() =
        runBlocking {
            val offline = executeSyncWork(true, API_URL) { throw IOException("offline") }
            val serverFailure = executeSyncWork(true, API_URL) { throw SyncHttpException(503) }

            assertEquals(SyncWorkOutcome.RETRY, offline)
            assertEquals(SyncWorkOutcome.RETRY, serverFailure)
        }

    @Test
    fun invalidConfigurationFailsAndSuccessfulPassCompletes() =
        runBlocking {
            val invalid = executeSyncWork(true, API_URL) { throw IllegalArgumentException("bad URL") }
            val success = executeSyncWork(true, API_URL) { assertTrue(true) }

            assertEquals(SyncWorkOutcome.FAILURE, invalid)
            assertEquals(SyncWorkOutcome.SUCCESS, success)
        }

    private companion object {
        const val API_URL = "https://sync.example.test"
    }
}
