package br.com.bnuuy.jwar.server.dto;

import java.util.UUID;

public record PlayerSnapshot(
    UUID userId,
    String displayName,
    String color,
    int troopsAvailable,
    int cardCount,
    boolean eliminated
) {}
