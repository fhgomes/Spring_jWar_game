package br.com.bnuuy.jwar.server.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomMessageDto(
    UUID id,
    UUID senderId,
    String senderName,
    String text,
    Instant sentAt
) {}
