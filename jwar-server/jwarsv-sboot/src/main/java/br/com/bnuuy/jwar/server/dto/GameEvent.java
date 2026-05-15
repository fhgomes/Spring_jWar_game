package br.com.bnuuy.jwar.server.dto;

import java.time.Instant;
import java.util.UUID;

public record GameEvent(
    GameEventType type,
    UUID matchId,
    Object payload,
    Instant timestamp
) {

    public static GameEvent of(GameEventType type, UUID matchId, Object payload) {
        return new GameEvent(type, matchId, payload, Instant.now());
    }
}
