-- jWar V2: rooms, room_members, room_messages
-- Lobbies for players to assemble before matches start.

CREATE TABLE rooms (
    id              UUID PRIMARY KEY,
    name            VARCHAR(80)   NOT NULL,
    host_user_id    UUID          NOT NULL REFERENCES users(id),
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    max_players     INTEGER       NOT NULL,
    password_hash   VARCHAR(120),
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP
);

CREATE INDEX idx_rooms_status ON rooms (status);
CREATE INDEX idx_rooms_host ON rooms (host_user_id);

CREATE TABLE room_members (
    room_id     UUID         NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID         NOT NULL REFERENCES users(id),
    color       VARCHAR(20)  NOT NULL,
    is_host     BOOLEAN      NOT NULL DEFAULT FALSE,
    joined_at   TIMESTAMP    NOT NULL,
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT uq_room_color UNIQUE (room_id, color)
);

CREATE INDEX idx_room_members_user ON room_members (user_id);

CREATE TABLE room_messages (
    id              UUID PRIMARY KEY,
    room_id         UUID         NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_user_id  UUID         NOT NULL REFERENCES users(id),
    text            VARCHAR(280) NOT NULL,
    sent_at         TIMESTAMP    NOT NULL
);

CREATE INDEX idx_room_messages_room_time ON room_messages (room_id, sent_at);
