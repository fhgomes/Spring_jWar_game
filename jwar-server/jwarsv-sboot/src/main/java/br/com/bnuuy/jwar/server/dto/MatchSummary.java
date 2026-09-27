package br.com.bnuuy.jwar.server.dto;

import br.com.bnuuy.jwar.server.domain.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record MatchSummary(
    UUID matchId,
    UUID roomId,
    MatchStatus status,
    Instant startedAt,
    Instant finishedAt,
    UUID winnerUserId,
    UUID currentTurnUserId,
    String currentPhase
) {}
