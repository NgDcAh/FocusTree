package com.focustree.server

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerRoutesTest {
    @Test
    fun `health register authenticate upload duplicate and revoke routes work`() =
        testApplication {
            val repository = RouteFakeRepository()
            val service = SyncService(repository) { TOKEN }
            application { configureServer(service) }

            assertEquals(HttpStatusCode.OK, client.get("/health").status)
            val registrationResponse = client.post("/v1/installations")
            assertEquals(HttpStatusCode.Created, registrationResponse.status)
            val registration = Json.decodeFromString<InstallationResponse>(registrationResponse.bodyAsText())

            assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/sessions").status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .get("/v1/sessions?limit=invalid") {
                        header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                    }.status,
            )
            val upload = SessionUpload(SESSION_ID, "reading", 1_500_000, 100, 1_500_100, "COMPLETED")
            val first =
                client.post("/v1/sessions") {
                    header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(SessionUpload.serializer(), upload))
                }
            assertEquals(HttpStatusCode.OK, first.status)
            assertTrue(first.bodyAsText().contains("\"duplicate\":false"))
            val second =
                client.post("/v1/sessions") {
                    header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(SessionUpload.serializer(), upload))
                }
            assertTrue(second.bodyAsText().contains("\"duplicate\":true"))

            val page =
                client.get("/v1/sessions?limit=10") {
                    header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                }
            assertEquals(HttpStatusCode.OK, page.status)
            assertTrue(page.bodyAsText().contains("reading"))

            val revoked =
                client.delete("/v1/installations/me") {
                    header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                }
            assertEquals(HttpStatusCode.NoContent, revoked.status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                client
                    .get("/v1/sessions") {
                        header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                    }.status,
            )
        }

    @Test
    fun `invalid finalized session is a client error and never stored`() =
        testApplication {
            val repository = RouteFakeRepository()
            application { configureServer(SyncService(repository) { TOKEN }) }
            val registration =
                Json.decodeFromString<InstallationResponse>(
                    client.post("/v1/installations").bodyAsText(),
                )
            val response =
                client.post("/v1/sessions") {
                    header(HttpHeaders.Authorization, "Bearer ${registration.bearerToken}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"clientSessionId":"$SESSION_ID","intention":"","plannedDurationMillis":1000,"startedAtEpochMillis":0,"finishedAtEpochMillis":1000,"outcome":"ACTIVE"}""",
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(repository.sessions.isEmpty())
        }

    private class RouteFakeRepository : SyncRepository {
        private val tokens = mutableMapOf<String, java.util.UUID>()
        val sessions = mutableMapOf<Pair<java.util.UUID, java.util.UUID>, StoredSession>()

        override fun registerInstallation(
            installationId: java.util.UUID,
            tokenHash: ByteArray,
        ) {
            tokens[tokenHash.joinToString()] = installationId
        }

        override fun installationOwnsToken(tokenHash: ByteArray): java.util.UUID? = tokens[tokenHash.joinToString()]

        override fun revokeInstallation(installationId: java.util.UUID) {
            tokens.entries.removeAll { it.value == installationId }
        }

        override fun upsertSession(
            installationId: java.util.UUID,
            session: StoredSession,
        ): Boolean = sessions.putIfAbsent(installationId to session.clientSessionId, session) == null

        override fun pageSessions(
            installationId: java.util.UUID,
            limit: Int,
            beforeStartedAtEpochMillis: Long?,
            beforeSessionId: java.util.UUID?,
        ): List<StoredSession> = sessions.filterKeys { it.first == installationId }.values.take(limit)
    }

    private companion object {
        const val TOKEN = "route-test-token"
        const val SESSION_ID = "7c97c85e-4b62-4cda-a76d-bf61010e7207"
    }
}
