package br.com.bnuuy.jwar.server.dto;

import java.util.UUID;

public record ContinentSnapshot(
    int code,
    String name,
    UUID ownerUserId,
    int bonusTroops
) {}
