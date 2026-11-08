package com.focustree.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SyncServiceTest {
    @Test
    fun `installation token is returned once and only hash is stored`() {
        val repository = FakeRepository()
        val service = SyncService(repository) { TOKEN }
        val registration = service.register()
        assertEquals(TOKEN, registration.bearerToken)
        assertEquals(registration.installationId, service.authenticate(TOKEN).toString())
        assertNull(service.authenticate("wrong-token"))
        assertFalse(repository.storedTokenHashes.single().contentEquals(TOKEN.toByteArray()))
        assertEquals(TokenSecurity.sha256(TOKEN).toList(), repository.storedTokenHashes.single().toList())
    }

    @Test
    fun `revoke prevents further authentication`() {
        val repository = FakeRepository()
        val service = SyncService(repository) { TOKEN }
        val registration = service.register()
        service.revoke(UUID.fromString(registration.installationId))
        assertNull(service.authenticate(TOKEN))
    }

    @Test
    fun `only valid finalized outcomes and bounded inputs are accepted`() {
        val repository = FakeRepository()
        val service = SyncService(repository)
        val installation = UUID.randomUUID()
        assertThrows(InvalidSession::class.java) { service.accept(installation, upload(outcome = "ACTIVE")) }
        assertThrows(InvalidSession::class.java) { service.accept(installation, upload(duration = 4)) }
        assertThrows(InvalidSession::class.java) { service.accept(installation, upload(outcome = "COMPLETED", finish = 100L)) }
        assertThrows(InvalidSession::class.java) { service.accept(installation, upload(intention = "x".repeat(121))) }
        assertThrows(InvalidSession::class.java) { service.accept(installation, upload(sessionId = "nope")) }
        assertTrue(repository.sessions.isEmpty())
    }

    @Test
    fun `paging uses stable cursors and bounded page sizes`() {
        val repository = FakeRepository()
        val service = SyncService(repository)
        val installation = UUID.randomUUID()
        val first = service.page(installation, 10, null, null)
        assertNotNull(first)
        assertThrows(InvalidSession::class.java) { service.page(installation, 101, null, null) }
        assertThrows(InvalidSession::class.java) { service.page(installation, 10, 1, null) }
    }

    private fun upload(
        sessionId: String = SESSION_ID,
        intention: String = "write",
        duration: Int = 25,
        outcome: String = "COMPLETED",
        start: Long = 100L,
        finish: Long = start + duration * MINUTE_MILLIS,
    ) = SessionUpload(
        clientSessionId = sessionId,
        intention = intention,
        plannedDurationMillis = duration * MINUTE_MILLIS,
        startedAtEpochMillis = start,
        finishedAtEpochMillis = finish,
        outcome = outcome,
    )

    private class FakeRepository : SyncRepository {
        private val installations = mutableMapOf<UUID, ByteArray>()
        val storedTokenHashes = mutableListOf<ByteArray>()
        val sessions = mutableMapOf<Pair<UUID, UUID>, StoredSession>()

        override fun registerInstallation(
            installationId: UUID,
            tokenHash: ByteArray,
        ) {
            installations[installationId] = tokenHash
            storedTokenHashes += tokenHash
        }

        override fun installationOwnsToken(tokenHash: ByteArray): UUID? =
            installations.entries.firstOrNull { it.value.contentEquals(tokenHash) }?.key

        override fun revokeInstallation(installationId: UUID) {
            installations.remove(installationId)
        }

        override fun upsertSession(
            installationId: UUID,
            session: StoredSession,
        ): Boolean = sessions.putIfAbsent(installationId to session.clientSessionId, session) == null

        override fun pageSessions(
            installationId: UUID,
            limit: Int,
            beforeStartedAtEpochMillis: Long?,
            beforeSessionId: UUID?,
        ): List<StoredSession> = sessions.filterKeys { it.first == installationId }.values.take(limit)
    }

    private companion object {
        const val TOKEN = "local-test-token"
        const val SESSION_ID = "7c97c85e-4b62-4cda-a76d-bf61010e7207"
        const val MINUTE_MILLIS = 60_000L
    }
}
