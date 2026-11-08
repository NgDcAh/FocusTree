package com.focustree.server

import java.util.UUID

data class StoredSession(
    val clientSessionId: UUID,
    val intention: String,
    val plannedDurationMillis: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val outcome: SessionOutcome,
)

enum class SessionOutcome { COMPLETED, ABANDONED }

interface SyncRepository {
    fun registerInstallation(
        installationId: UUID,
        tokenHash: ByteArray,
    )

    fun installationOwnsToken(tokenHash: ByteArray): UUID?

    fun revokeInstallation(installationId: UUID)

    fun upsertSession(
        installationId: UUID,
        session: StoredSession,
    ): Boolean

    fun pageSessions(
        installationId: UUID,
        limit: Int,
        beforeStartedAtEpochMillis: Long?,
        beforeSessionId: UUID?,
    ): List<StoredSession>
}

class IdempotencyConflict : RuntimeException()

class SyncService(
    private val repository: SyncRepository,
    private val tokenGenerator: () -> String = TokenSecurity::newToken,
) {
    fun register(): InstallationResponse {
        val id = UUID.randomUUID()
        val token = tokenGenerator()
        repository.registerInstallation(id, TokenSecurity.sha256(token))
        return InstallationResponse(id.toString(), token)
    }

    fun authenticate(token: String?): UUID? {
        if (token.isNullOrBlank() || token.length > MAX_TOKEN_LENGTH) return null
        return repository.installationOwnsToken(TokenSecurity.sha256(token))
    }

    fun revoke(installationId: UUID) = repository.revokeInstallation(installationId)

    fun accept(
        installationId: UUID,
        upload: SessionUpload,
    ): Boolean {
        val sessionId =
            runCatching { UUID.fromString(upload.clientSessionId) }.getOrNull()
                ?: throw InvalidSession("invalid_session_id")
        if (upload.intention.length > MAX_INTENTION_LENGTH) throw InvalidSession("intention_too_long")
        if (upload.plannedDurationMillis !in MIN_DURATION_MILLIS..MAX_DURATION_MILLIS) {
            throw InvalidSession("invalid_duration")
        }
        if (upload.startedAtEpochMillis < 0 || upload.finishedAtEpochMillis < upload.startedAtEpochMillis) {
            throw InvalidSession("invalid_timestamps")
        }
        val outcome =
            runCatching { SessionOutcome.valueOf(upload.outcome) }.getOrNull()
                ?: throw InvalidSession("invalid_outcome")
        if (outcome == SessionOutcome.COMPLETED &&
            upload.finishedAtEpochMillis - upload.startedAtEpochMillis < upload.plannedDurationMillis
        ) {
            throw InvalidSession("incomplete_session")
        }
        return repository.upsertSession(
            installationId,
            StoredSession(
                sessionId,
                upload.intention,
                upload.plannedDurationMillis,
                upload.startedAtEpochMillis,
                upload.finishedAtEpochMillis,
                outcome,
            ),
        )
    }

    fun page(
        installationId: UUID,
        limit: Int,
        beforeStartedAtEpochMillis: Long?,
        beforeSessionId: String?,
    ): SessionPage {
        if (limit !in 1..MAX_PAGE_SIZE) throw InvalidSession("invalid_page_size")
        val cursorId = beforeSessionId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if ((beforeStartedAtEpochMillis == null) != (beforeSessionId == null) ||
            (beforeSessionId != null && cursorId == null)
        ) {
            throw InvalidSession("invalid_page_cursor")
        }
        val results = repository.pageSessions(installationId, limit + 1, beforeStartedAtEpochMillis, cursorId)
        val visible = results.take(limit)
        val cursor = visible.lastOrNull().takeIf { results.size > limit }
        return SessionPage(
            sessions = visible.map { it.toUpload() },
            nextBeforeStartedAtEpochMillis = cursor?.startedAtEpochMillis,
            nextBeforeSessionId = cursor?.clientSessionId?.toString(),
        )
    }

    private fun StoredSession.toUpload() =
        SessionUpload(
            clientSessionId = clientSessionId.toString(),
            intention = intention,
            plannedDurationMillis = plannedDurationMillis,
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = finishedAtEpochMillis,
            outcome = outcome.name,
        )

    private companion object {
        const val MAX_TOKEN_LENGTH = 256
        const val MAX_INTENTION_LENGTH = 120
        const val MIN_DURATION_MILLIS = 300_000L
        const val MAX_DURATION_MILLIS = 7_200_000L
        const val MAX_PAGE_SIZE = 100
    }
}

class InvalidSession(
    val code: String,
) : RuntimeException()
