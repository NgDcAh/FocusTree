package com.focustree.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import javax.sql.DataSource

class PostgresSyncRepository(
    private val dataSource: DataSource,
) : SyncRepository {
    override fun registerInstallation(
        installationId: UUID,
        tokenHash: ByteArray,
    ) {
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    "INSERT INTO installations (id, bearer_token_sha256) VALUES (?, ?)",
                ).use { statement ->
                    statement.setObject(1, installationId)
                    statement.setBytes(2, tokenHash)
                    statement.executeUpdate()
                }
        }
    }

    override fun installationOwnsToken(tokenHash: ByteArray): UUID? =
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    "SELECT id FROM installations WHERE bearer_token_sha256 = ? AND revoked_at IS NULL",
                ).use { statement ->
                    statement.setBytes(1, tokenHash)
                    statement.executeQuery().use { result ->
                        if (result.next()) result.getObject("id", UUID::class.java) else null
                    }
                }
        }

    override fun revokeInstallation(installationId: UUID) {
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    "UPDATE installations SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP) WHERE id = ?",
                ).use { statement ->
                    statement.setObject(1, installationId)
                    statement.executeUpdate()
                }
        }
    }

    override fun upsertSession(
        installationId: UUID,
        session: StoredSession,
    ): Boolean =
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val inserted =
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO focus_sessions (
                                installation_id, client_session_id, intention, planned_duration_millis,
                                started_at_epoch_millis, finished_at_epoch_millis, outcome
                            ) VALUES (?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (installation_id, client_session_id) DO NOTHING
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setObject(1, installationId)
                            statement.setObject(2, session.clientSessionId)
                            statement.setString(3, session.intention)
                            statement.setLong(4, session.plannedDurationMillis)
                            statement.setLong(5, session.startedAtEpochMillis)
                            statement.setLong(6, session.finishedAtEpochMillis)
                            statement.setString(7, session.outcome.name)
                            statement.executeUpdate() == 1
                        }
                if (!inserted && !existingSessionMatches(connection, installationId, session)) {
                    throw IdempotencyConflict()
                }
                connection.commit()
                inserted
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            }
        }

    override fun pageSessions(
        installationId: UUID,
        limit: Int,
        beforeStartedAtEpochMillis: Long?,
        beforeSessionId: UUID?,
    ): List<StoredSession> =
        dataSource.connection.use { connection ->
            val hasCursor = beforeStartedAtEpochMillis != null && beforeSessionId != null
            val cursorClause = if (hasCursor) "AND (started_at_epoch_millis, client_session_id) < (?, ?)" else ""
            val sql =
                """
                SELECT client_session_id, intention, planned_duration_millis,
                       started_at_epoch_millis, finished_at_epoch_millis, outcome
                FROM focus_sessions
                WHERE installation_id = ? $cursorClause
                ORDER BY started_at_epoch_millis DESC, client_session_id DESC
                LIMIT ?
                """.trimIndent()
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, installationId)
                var index = 2
                if (hasCursor) {
                    statement.setLong(index++, beforeStartedAtEpochMillis!!)
                    statement.setObject(index++, beforeSessionId)
                }
                statement.setInt(index, limit)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(
                                StoredSession(
                                    result.getObject("client_session_id", UUID::class.java),
                                    result.getString("intention"),
                                    result.getLong("planned_duration_millis"),
                                    result.getLong("started_at_epoch_millis"),
                                    result.getLong("finished_at_epoch_millis"),
                                    SessionOutcome.valueOf(result.getString("outcome")),
                                ),
                            )
                        }
                    }
                }
            }
        }

    private fun existingSessionMatches(
        connection: java.sql.Connection,
        installationId: UUID,
        session: StoredSession,
    ): Boolean =
        connection
            .prepareStatement(
                """
                SELECT intention, planned_duration_millis, started_at_epoch_millis,
                       finished_at_epoch_millis, outcome
                FROM focus_sessions WHERE installation_id = ? AND client_session_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, installationId)
                statement.setObject(2, session.clientSessionId)
                statement.executeQuery().use { result ->
                    result.next() &&
                        result.getString("intention") == session.intention &&
                        result.getLong("planned_duration_millis") == session.plannedDurationMillis &&
                        result.getLong("started_at_epoch_millis") == session.startedAtEpochMillis &&
                        result.getLong("finished_at_epoch_millis") == session.finishedAtEpochMillis &&
                        result.getString("outcome") == session.outcome.name
                }
            }
}

class DatabaseResources(
    jdbcUrl: String,
    username: String,
    password: String,
) : AutoCloseable {
    private val pool =
        HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = username
                this.password = password
                maximumPoolSize = 8
                poolName = "focus-tree-server"
            },
        )
    val repository = PostgresSyncRepository(pool)

    init {
        org.flywaydb.core.Flyway
            .configure()
            .dataSource(pool)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    override fun close() = pool.close()
}

data class ServerSettings(
    val port: Int,
    val jdbcUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val tlsKeyStorePath: String?,
    val tlsKeyStorePassword: String?,
    val tlsKeyAlias: String?,
    val tlsPrivateKeyPassword: String?,
    val tlsPort: Int,
) {
    init {
        val tlsSettings = listOf(tlsKeyStorePath, tlsKeyStorePassword, tlsKeyAlias, tlsPrivateKeyPassword)
        require(tlsSettings.all { it == null } || tlsSettings.all { it != null }) {
            "All local TLS settings must be provided together."
        }
    }

    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()) =
            ServerSettings(
                port = environment["PORT"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_PORT,
                jdbcUrl = environment["DATABASE_URL"] ?: error("DATABASE_URL is required"),
                databaseUser = environment["DB_USER"] ?: error("DB_USER is required"),
                databasePassword = environment["DB_PASSWORD"] ?: error("DB_PASSWORD is required"),
                tlsKeyStorePath = environment["TLS_KEYSTORE_PATH"],
                tlsKeyStorePassword = environment["TLS_KEYSTORE_PASSWORD"],
                tlsKeyAlias = environment["TLS_KEY_ALIAS"],
                tlsPrivateKeyPassword = environment["TLS_PRIVATE_KEY_PASSWORD"],
                tlsPort = environment["TLS_PORT"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_TLS_PORT,
            )

        const val DEFAULT_PORT = 8080
        const val DEFAULT_TLS_PORT = 8443
    }
}
