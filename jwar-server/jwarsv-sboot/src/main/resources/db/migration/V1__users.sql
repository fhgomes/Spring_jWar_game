-- jWar V1: users table
-- Persistent identity row keyed by Firebase UID.

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    firebase_uid    VARCHAR(128) NOT NULL UNIQUE,
    email           VARCHAR(320) NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    display_name    VARCHAR(80),
    photo_url       VARCHAR(2048),
    provider        VARCHAR(64)  NOT NULL DEFAULT 'password',
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_users_email ON users (email);
