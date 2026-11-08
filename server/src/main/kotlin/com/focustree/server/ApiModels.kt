package com.focustree.server

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class InstallationResponse(
    val installationId: String,
    val bearerToken: String,
)

@Serializable
data class SessionUpload(
    val clientSessionId: String,
    val intention: String,
    val plannedDurationMillis: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val outcome: String,
)

@Serializable
data class SessionPage(
    val sessions: List<SessionUpload>,
    val nextBeforeStartedAtEpochMillis: Long? = null,
    val nextBeforeSessionId: String? = null,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

@Serializable
data class SessionUpsertResponse(
    val duplicate: Boolean,
)
