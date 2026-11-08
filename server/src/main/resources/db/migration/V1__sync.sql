CREATE TABLE installations (
    id UUID PRIMARY KEY,
    bearer_token_sha256 BYTEA NOT NULL UNIQUE CHECK (octet_length(bearer_token_sha256) = 32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE focus_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    installation_id UUID NOT NULL REFERENCES installations(id),
    client_session_id UUID NOT NULL,
    intention VARCHAR(120) NOT NULL DEFAULT '',
    planned_duration_millis BIGINT NOT NULL CHECK (planned_duration_millis BETWEEN 300000 AND 7200000),
    started_at_epoch_millis BIGINT NOT NULL CHECK (started_at_epoch_millis >= 0),
    finished_at_epoch_millis BIGINT NOT NULL CHECK (finished_at_epoch_millis >= started_at_epoch_millis),
    outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('COMPLETED', 'ABANDONED')),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (installation_id, client_session_id)
);

CREATE INDEX focus_sessions_page_idx
    ON focus_sessions (installation_id, started_at_epoch_millis DESC, client_session_id DESC);
