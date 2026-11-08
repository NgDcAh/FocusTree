package com.focustree.feature.sync

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RemoteSession(
    val id: String,
    val intention: String,
    val plannedDurationMillis: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val outcome: SessionOutcome,
)

data class RemotePage(
    val sessions: List<RemoteSession>,
    val beforeStartedAt: Long?,
    val beforeId: String?,
)

interface SyncApi {
    fun register(): SyncCredentials

    fun upload(
        credentials: SyncCredentials,
        session: FocusSession,
    )

    fun download(
        credentials: SyncCredentials,
        beforeStartedAt: Long?,
        beforeId: String?,
    ): RemotePage

    fun revoke(credentials: SyncCredentials)
}

class SyncHttpException(
    val statusCode: Int,
    val errorCode: String? = null,
) : java.io.IOException("Sync API returned HTTP $statusCode${errorCode?.let { " ($it)" } ?: ""}")

class HttpsSyncApi(
    baseUrl: String,
) : SyncApi {
    private val base: URL =
        URL(baseUrl.trimEnd('/') + "/").also {
            require(it.protocol.equals("https", true)) { "Sync API must use HTTPS" }
            require(it.host.isNotBlank() && it.userInfo == null) { "Invalid sync API URL" }
        }

    override fun register(): SyncCredentials {
        val json = request("POST", "v1/installations", null, JSONObject())
        return SyncCredentials(json.getString("installationId"), json.getString("bearerToken"))
    }

    override fun upload(
        credentials: SyncCredentials,
        session: FocusSession,
    ) {
        val payload =
            JSONObject()
                .put("clientSessionId", session.id)
                .put("intention", session.intention)
                .put("plannedDurationMillis", session.plannedDurationMillis)
                .put("startedAtEpochMillis", session.startedAtEpochMillis)
                .put("finishedAtEpochMillis", session.finishedAtEpochMillis ?: session.startedAtEpochMillis)
                .put("outcome", session.outcome.name)
        request("POST", "v1/sessions", credentials, payload)
    }

    override fun download(
        credentials: SyncCredentials,
        beforeStartedAt: Long?,
        beforeId: String?,
    ): RemotePage {
        val query =
            if (beforeStartedAt == null || beforeId == null) {
                "?limit=50"
            } else {
                "?limit=50&beforeStartedAtEpochMillis=$beforeStartedAt&beforeSessionId=$beforeId"
            }
        val body = request("GET", "v1/sessions$query", credentials, null)
        val sessions = body.getJSONArray("sessions").toRemoteSessions()
        return RemotePage(
            sessions,
            body.optNullableLong("nextBeforeStartedAtEpochMillis"),
            body.optString("nextBeforeSessionId").takeIf(String::isNotEmpty),
        )
    }

    override fun revoke(credentials: SyncCredentials) {
        request("DELETE", "v1/installations/me", credentials, null, allowEmpty = true)
    }

    private fun request(
        method: String,
        path: String,
        credentials: SyncCredentials?,
        body: JSONObject?,
        allowEmpty: Boolean = false,
    ): JSONObject {
        val connection = URL(base, path).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        credentials?.let { connection.setRequestProperty("Authorization", "Bearer ${it.bearerToken}") }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
        }
        try {
            val status = connection.responseCode
            if (status !in FIRST_SUCCESS_STATUS..LAST_SUCCESS_STATUS) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val errorCode =
                    if (errorBody == null) {
                        null
                    } else {
                        runCatching {
                            JSONObject(errorBody).optString("code").takeIf(String::isNotEmpty)
                        }.getOrNull()
                    }
                throw SyncHttpException(status, errorCode)
            }
            if (allowEmpty || status == HttpURLConnection.HTTP_NO_CONTENT) return JSONObject()
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.toRemoteSessions(): List<RemoteSession> =
        List(length()) { index ->
            val item = getJSONObject(index)
            val outcome = SessionOutcome.valueOf(item.getString("outcome"))
            require(outcome == SessionOutcome.COMPLETED || outcome == SessionOutcome.ABANDONED)
            RemoteSession(
                item.getString("clientSessionId"),
                item.getString("intention"),
                item.getLong("plannedDurationMillis"),
                item.getLong("startedAtEpochMillis"),
                item.getLong("finishedAtEpochMillis"),
                outcome,
            )
        }

    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        const val FIRST_SUCCESS_STATUS = 200
        const val LAST_SUCCESS_STATUS = 299
    }
}
