package com.focustree.server

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID

class PostgresSyncIntegrationTest {
    @Test
    fun migrationAuthenticationIdempotencyPagingAndRevocationWorkAgainstPostgres() {
        DatabaseResources(postgres.jdbcUrl, postgres.username, postgres.password).use { database ->
            val repository = database.repository
            val service = SyncService(repository) { TEST_TOKEN }
            val installation = service.register()
            val installationId = UUID.fromString(installation.installationId)
            assertEquals(installationId, service.authenticate(TEST_TOKEN))

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success = TRUE").use { result ->
                        assertTrue(result.next() && result.getInt(1) >= 1)
                    }
                }
            }

            assertThrows(InvalidSession::class.java) {
                service.accept(installationId, upload(SESSION_1, "ACTIVE", 50))
            }

            val first = service.accept(installationId, upload(SESSION_1, "COMPLETED", 100))
            val duplicate = service.accept(installationId, upload(SESSION_1, "COMPLETED", 100))
            assertTrue(first)
            assertFalse(duplicate)
            assertThrows(IdempotencyConflict::class.java) {
                service.accept(installationId, upload(SESSION_1, "COMPLETED", 101))
            }
            service.accept(installationId, upload(SESSION_2, "ABANDONED", 200))

            val firstPage = service.page(installationId, 1, null, null)
            assertEquals(listOf(SESSION_2), firstPage.sessions.map { it.clientSessionId })
            val secondPage =
                service.page(
                    installationId,
                    1,
                    firstPage.nextBeforeStartedAtEpochMillis,
                    firstPage.nextBeforeSessionId,
                )
            assertEquals(listOf(SESSION_1), secondPage.sessions.map { it.clientSessionId })

            service.revoke(installationId)
            assertEquals(null, service.authenticate(TEST_TOKEN))
            assertEquals(2, service.page(installationId, 10, null, null).sessions.size)
        }
    }

    private fun upload(
        sessionId: String,
        outcome: String,
        startedAt: Long,
    ) = SessionUpload(
        clientSessionId = sessionId,
        intention = "integration focus",
        plannedDurationMillis = DURATION_MILLIS,
        startedAtEpochMillis = startedAt,
        finishedAtEpochMillis = startedAt + DURATION_MILLIS,
        outcome = outcome,
    )

    companion object {
        private lateinit var postgres: PostgreSQLContainer<*>
        private const val TEST_TOKEN = "postgres-integration-test-token"
        private const val SESSION_1 = "45b41373-f5e6-4dc0-8d16-bf48846a9a18"
        private const val SESSION_2 = "d1879168-8624-44f5-a026-c0e437b22a5a"
        private const val DURATION_MILLIS = 1_500_000L

        @BeforeClass
        @JvmStatic
        fun startPostgres() {
            postgres =
                PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("focus_tree_test")
                    .withUsername("test_user")
                    .withPassword("test_password")
            postgres.start()
        }

        @AfterClass
        @JvmStatic
        fun stopPostgres() {
            if (::postgres.isInitialized) postgres.stop()
        }
    }
}
