-- jWar V3: matches metadata
-- Persistent metadata for history and listing. The in-flight ClassicGame is held in memory.

CREATE TABLE matches (
    id                       UUID PRIMARY KEY,
    room_id                  UUID         NOT NULL REFERENCES rooms(id),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    started_at               TIMESTAMP    NOT NULL,
    finished_at              TIMESTAMP,
    winner_user_id           UUID         REFERENCES users(id),
    current_turn_user_id     UUID         REFERENCES users(id),
    current_phase            VARCHAR(20)
);

CREATE INDEX idx_matches_room ON matches (room_id);
CREATE INDEX idx_matches_status ON matches (status);
