package br.com.bnuuy.jwar.server.dto;

import java.util.UUID;

public record CountrySnapshot(
    int code,
    String name,
    int continentCode,
    UUID ownerUserId,
    int troops
) {}
