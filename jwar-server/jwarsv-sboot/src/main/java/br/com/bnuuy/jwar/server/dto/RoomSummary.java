package br.com.bnuuy.jwar.server.dto;

import br.com.bnuuy.jwar.server.domain.RoomStatus;
import java.time.Instant;
import java.util.UUID;

public record RoomSummary(
    UUID id,
    String name,
    String hostNickname,
    int memberCount,
    int maxPlayers,
    RoomStatus status,
    boolean hasPassword,
    Instant createdAt
) {}
