-- Unified Google Workspace connection (Gmail + Calendar/Meet + Tasks) — replaces the old
-- per-service connected_google_calendars / connected_mailboxes(provider=GMAIL) OAuth rows.
CREATE TABLE IF NOT EXISTS google_workspace_credentials (
    id                    BIGSERIAL PRIMARY KEY,
    owner                 VARCHAR(255) NOT NULL UNIQUE,
    organization_id       VARCHAR(255),
    google_user_id        VARCHAR(255),
    email                 VARCHAR(255),
    access_token          TEXT,
    refresh_token         TEXT,
    token_expires_at       TIMESTAMP,
    granted_scopes        TEXT,
    token_type            VARCHAR(64),
    connected             BOOLEAN NOT NULL DEFAULT TRUE,
    revoked               BOOLEAN NOT NULL DEFAULT FALSE,
    connected_at          TIMESTAMP,
    updated_at            TIMESTAMP,
    last_token_refresh_at TIMESTAMP,
    last_api_success_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_google_workspace_credentials_owner ON google_workspace_credentials (owner);
