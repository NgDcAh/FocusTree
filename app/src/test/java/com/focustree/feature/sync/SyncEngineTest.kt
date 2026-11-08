package com.focustree.feature.sync

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncEngineTest {
    @Test
    fun registersUploadsAndDownloadsIdempotently() =
        runBlocking {
            val fixture = Fixture()
            fixture.api.remotePages += RemotePage(listOf(remote("remote")), null, null)

            fixture.engine.runOnce()
            fixture.engine.runOnce()

            assertEquals(1, fixture.api.registrations)
            assertEquals(listOf("local"), fixture.api.uploadedIds)
            assertEquals(1, fixture.store.acknowledged.size)
            assertEquals(listOf("remote"), fixture.store.imported.map { it.id })
            assertEquals(
                true,
                fixture.store.imported
                    .single()
                    .rewardGranted,
            )
        }

    @Test
    fun failedUploadStaysQueuedAndRecordsRetry() =
        runBlocking {
            val fixture = Fixture().apply { api.uploadFailure = SyncHttpException(503) }

            runCatching { fixture.engine.runOnce() }

            assertTrue(fixture.store.acknowledged.isEmpty())
            assertEquals("503", fixture.store.failures["local"])
        }

    @Test
    fun unauthorizedUploadClearsRevokedCredential() =
        runBlocking {
            val fixture =
                Fixture().apply {
                    credential.value = SyncCredentials("install", "old-token")
                    api.uploadFailure = SyncHttpException(401)
                }

            runCatching { fixture.engine.runOnce() }

            assertNull(fixture.credential.value)
            assertEquals("401", fixture.store.failures["local"])
        }

    @Test
    fun offlineUploadRemainsQueuedForRetry() =
        runBlocking {
            val fixture = Fixture().apply { api.uploadNetworkFailure = IOException("offline") }

            runCatching { fixture.engine.runOnce() }

            assertEquals(listOf("local"), fixture.store.pending.map { it.id })
            assertEquals("network", fixture.store.failures["local"])
        }

    @Test
    fun newEngineInstanceReusesPersistedInstallationCredential() =
        runBlocking {
            val fixture = Fixture()
            fixture.engine.runOnce()
            val recreatedEngine = SyncEngine(fixture.api, fixture.credential, fixture.store)

            recreatedEngine.runOnce()

            assertEquals(1, fixture.api.registrations)
        }

    @Test
    fun unauthorizedDownloadClearsRevokedCredential() =
        runBlocking {
            val fixture =
                Fixture().apply {
                    credential.value = SyncCredentials("install", "revoked")
                    api.downloadFailure = SyncHttpException(401)
                }

            runCatching { fixture.engine.runOnce() }

            assertNull(fixture.credential.value)
        }

    @Test
    fun retryAfterTransientFailureCompletesUploadWhenServiceReturns() =
        runBlocking {
            val fixture = Fixture().apply { api.uploadFailure = SyncHttpException(503) }
            runCatching { fixture.engine.runOnce() }

            fixture.api.uploadFailure = null
            fixture.engine.runOnce()

            assertEquals(listOf("local"), fixture.api.uploadedIds)
            assertTrue(fixture.store.pending.isEmpty())
            assertEquals(listOf("local"), fixture.store.acknowledged)
        }

    @Test
    fun invalidTokenIsReplacedWithANewAnonymousInstallationOnNextPass() =
        runBlocking {
            val fixture = Fixture()
            fixture.credential.value = SyncCredentials("revoked-install", "revoked-token")
            fixture.api.uploadFailure = SyncHttpException(401)
            runCatching { fixture.engine.runOnce() }

            fixture.api.uploadFailure = null
            fixture.engine.runOnce()

            assertEquals(1, fixture.api.registrations)
            assertEquals("token", fixture.credential.value?.bearerToken)
            assertTrue(fixture.store.pending.isEmpty())
        }

    private class Fixture {
        val credential = MemoryCredentials()
        val store = MemorySessions()
        val api = FakeApi()
        val engine = SyncEngine(api, credential, store)

        init {
            store.pending += local("local")
        }
    }

    private class MemoryCredentials : SyncCredentialStore {
        var value: SyncCredentials? = null

        override fun read() = value

        override fun write(credentials: SyncCredentials) {
            value = credentials
        }

        override fun clear() {
            value = null
        }
    }

    private class MemorySessions : SyncSessionRepository {
        val pending = mutableListOf<FocusSession>()
        val acknowledged = mutableListOf<String>()
        val failures = mutableMapOf<String, String>()
        val imported = mutableListOf<FocusSession>()

        override suspend fun pendingSyncSessions() = pending.toList()

        override suspend fun recordSyncFailure(
            sessionId: String,
            errorCode: String,
        ) {
            failures[sessionId] = errorCode
        }

        override suspend fun acknowledgeSync(sessionId: String) {
            acknowledged += sessionId
            pending.removeAll { it.id == sessionId }
        }

        override suspend fun recordRemoteSessions(sessions: List<FocusSession>) {
            sessions.filterNot { incoming -> imported.any { it.id == incoming.id } }.forEach(imported::add)
        }
    }

    private class FakeApi : SyncApi {
        var registrations = 0
        var uploadFailure: SyncHttpException? = null
        var uploadNetworkFailure: IOException? = null
        var downloadFailure: IOException? = null
        val uploadedIds = mutableListOf<String>()
        val remotePages = ArrayDeque<RemotePage>()

        override fun register(): SyncCredentials {
            registrations++
            return SyncCredentials("install", "token")
        }

        override fun upload(
            credentials: SyncCredentials,
            session: FocusSession,
        ) {
            uploadNetworkFailure?.let { throw it }
            uploadFailure?.let { throw it }
            uploadedIds += session.id
        }

        override fun download(
            credentials: SyncCredentials,
            beforeStartedAt: Long?,
            beforeId: String?,
        ): RemotePage {
            downloadFailure?.let { throw it }
            return remotePages.removeFirstOrNull() ?: RemotePage(emptyList(), null, null)
        }

        override fun revoke(credentials: SyncCredentials) = Unit
    }
}

private fun local(id: String) = FocusSession(id, "Focus", 60_000, 1_000, 61_000, 61_000, SessionOutcome.COMPLETED, true)

private fun remote(id: String) = RemoteSession(id, "Remote", 60_000, 1_000, 61_000, SessionOutcome.COMPLETED)
