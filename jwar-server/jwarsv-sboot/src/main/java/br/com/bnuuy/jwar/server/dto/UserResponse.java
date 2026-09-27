package br.com.bnuuy.jwar.server.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String firebaseUid,
    String email,
    String displayName,
    String photoUrl,
    String provider,
    Instant createdAt
) {}
