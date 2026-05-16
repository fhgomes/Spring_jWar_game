package br.com.bnuuy.jwar.server.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomMemberDto(
    UUID userId,
    String displayName,
    String color,
    boolean isHost,
    Instant joinedAt
) {}
