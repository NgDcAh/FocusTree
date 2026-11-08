package com.focustree.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.security.KeyStore

fun Application.configureServer(service: SyncService) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = false
                explicitNulls = false
            },
        )
    }
    install(StatusPages) {
        exception<InvalidSession> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(code = cause.code, message = "The request is invalid."))
        }
        exception<IdempotencyConflict> { call, _ ->
            call.respond(HttpStatusCode.Conflict, ApiError("session_conflict", "This session ID already has different data."))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_request", "The request body is invalid."))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_request", "The request body is invalid."))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ApiError("server_error", "The request could not be completed."))
        }
    }
    routing {
        get("/health") {
            call.respond(HealthResponse("ok"))
        }
        post("/v1/installations") {
            call.respond(HttpStatusCode.Created, service.register())
        }
        delete("/v1/installations/me") {
            val installationId = service.authenticate(call.authToken()) ?: return@delete call.respondUnauthorized()
            service.revoke(installationId)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/v1/sessions") {
            val installationId = service.authenticate(call.authToken()) ?: return@post call.respondUnauthorized()
            val payload = call.receive<SessionUpload>()
            val duplicate = !service.accept(installationId, payload)
            call.respond(SessionUpsertResponse(duplicate))
        }
        get("/v1/sessions") {
            val installationId = service.authenticate(call.authToken()) ?: return@get call.respondUnauthorized()
            val rawLimit = call.request.queryParameters["limit"]
            val limitValue =
                rawLimit?.toIntOrNull() ?: if (rawLimit == null) DEFAULT_PAGE_SIZE else throw InvalidSession("invalid_page_size")
            val beforeValue = call.request.queryParameters["beforeStartedAtEpochMillis"]?.toLongOrNull()
            if (call.request.queryParameters.contains("beforeStartedAtEpochMillis") && beforeValue == null) {
                throw InvalidSession("invalid_page_cursor")
            }
            call.respond(
                service.page(
                    installationId,
                    limitValue,
                    beforeValue,
                    call.request.queryParameters["beforeSessionId"],
                ),
            )
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondUnauthorized() =
    respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "A valid installation token is required."))

private fun io.ktor.server.application.ApplicationCall.authToken(): String? {
    val value = request.headers["Authorization"] ?: return null
    val prefix = "Bearer "
    if (!value.startsWith(prefix, ignoreCase = true)) return null
    return value.substring(prefix.length).trim().takeIf(String::isNotEmpty)
}

private const val DEFAULT_PAGE_SIZE = 50

fun main() {
    val settings = ServerSettings.fromEnvironment()
    val database = DatabaseResources(settings.jdbcUrl, settings.databaseUser, settings.databasePassword)
    val service = SyncService(database.repository)
    Runtime.getRuntime().addShutdownHook(Thread { database.close() })
    val tlsPath = settings.tlsKeyStorePath
    val tlsStorePassword = settings.tlsKeyStorePassword
    val tlsAlias = settings.tlsKeyAlias
    val tlsPrivatePassword = settings.tlsPrivateKeyPassword
    if (tlsPath != null && tlsStorePassword != null && tlsAlias != null && tlsPrivatePassword != null) {
        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(tlsPath).use { keyStore.load(it, tlsStorePassword.toCharArray()) }
        embeddedServer(
            Netty,
            configure = {
                sslConnector(
                    keyStore,
                    tlsAlias,
                    { tlsStorePassword.toCharArray() },
                    { tlsPrivatePassword.toCharArray() },
                ) {
                    host = "0.0.0.0"
                    port = settings.tlsPort
                }
            },
        ) { configureServer(service) }.start(wait = true)
    } else {
        embeddedServer(Netty, host = "0.0.0.0", port = settings.port) {
            configureServer(service)
        }.start(wait = true)
    }
}
