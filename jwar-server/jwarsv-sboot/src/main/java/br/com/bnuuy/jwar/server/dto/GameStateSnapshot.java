package br.com.bnuuy.jwar.server.dto;

import br.com.bnuuy.jwar.server.domain.MatchStatus;
import java.util.List;
import java.util.UUID;

public record GameStateSnapshot(
    UUID matchId,
    MatchStatus status,
    UUID currentTurnUserId,
    String currentPhase,
    List<PlayerSnapshot> players,
    List<CountrySnapshot> countries,
    List<ContinentSnapshot> continents,
    int turnNumber
) {}
