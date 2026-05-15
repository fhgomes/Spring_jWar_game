package br.com.bnuuy.jwar.server.dto;

public record AttackResultDto(
    int srcCountry,
    int targetCountry,
    int[] attackers,
    int[] defense,
    int srcCountryLoss,
    int targetCountryLoss,
    boolean conquered,
    boolean playerDestroyed
) {}
