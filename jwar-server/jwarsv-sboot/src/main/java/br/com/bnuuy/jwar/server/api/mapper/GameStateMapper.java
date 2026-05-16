package br.com.bnuuy.jwar.server.api.mapper;

import br.com.bnuuy.jwar.core.game.ClassicGame;
import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicContinents;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.server.domain.MatchStatus;
import br.com.bnuuy.jwar.server.dto.AttackResultDto;
import br.com.bnuuy.jwar.server.dto.ContinentSnapshot;
import br.com.bnuuy.jwar.server.dto.CountrySnapshot;
import br.com.bnuuy.jwar.server.dto.GameStateSnapshot;
import br.com.bnuuy.jwar.server.dto.PlayerSnapshot;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates the in-memory ClassicGame into immutable HTTP DTOs. This is the only
 * place that touches both the core engine and HTTP layer.
 */
@Component
@RequiredArgsConstructor
public class GameStateMapper {

    private final UserRepository userRepository;

    public GameStateSnapshot toSnapshot(UUID matchId, ClassicGame game) {
        if (game == null) {
            return new GameStateSnapshot(matchId, MatchStatus.IN_PROGRESS, null, null,
                List.of(), List.of(), List.of(), 0);
        }
        int qtdPlayers = game.getQtdPlayers();
        Map<Integer, UUID> playerSeqToUserId = new HashMap<>();
        List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (int seq = 1; seq <= qtdPlayers; seq++) {
            ClassicGamePlayer p = game.getPlayer(seq);
            if (p == null) continue;
            UUID userId = parseUserId(p.getUserId());
            playerSeqToUserId.put(seq, userId);
            String name = p.getNickName();
            if (userId != null) {
                name = userRepository.findById(userId)
                    .map(br.com.bnuuy.jwar.server.domain.User::getDisplayName)
                    .orElse(name);
            }
            String color = p.getColor() != null ? p.getColor().name() : null;
            playerSnapshots.add(new PlayerSnapshot(
                userId,
                name,
                color,
                p.getAvailableTroops(),
                p.getCardCount(),
                p.getOwnedCountries() == null || p.getOwnedCountries().isEmpty()
            ));
        }

        List<CountrySnapshot> countrySnapshots = new ArrayList<>();
        java.util.Set<Integer> seenCodes = new java.util.HashSet<>();
        for (EClassicCountries country : EClassicCountries.values()) {
            if (!seenCodes.add(country.getCode())) continue;
            ClassicGameCountry c = game.getCountry(country.getCode());
            if (c == null) continue;
            UUID ownerUserId = playerSeqToUserId.get(c.getOwnerCode());
            countrySnapshots.add(new CountrySnapshot(
                country.getCode(),
                c.getCountry() != null ? c.getCountry().getName() : country.getName(),
                country.getContinent().getCode(),
                ownerUserId,
                c.getTroopsCount()
            ));
        }

        List<ContinentSnapshot> continentSnapshots = new ArrayList<>();
        for (EClassicContinents continent : EClassicContinents.values()) {
            ClassicGameContinent c = game.getContinent(continent.getCode());
            if (c == null) continue;
            UUID ownerUserId = playerSeqToUserId.get(c.getGamePlayerOwner());
            continentSnapshots.add(new ContinentSnapshot(
                continent.getCode(),
                continent.getName(),
                ownerUserId,
                c.getAvailableTroopsCount()
            ));
        }

        UUID currentTurnUserId = playerSeqToUserId.get(game.getCurrentPlayer());
        String phase = phaseToString(game.getTurnPhase());
        MatchStatus matchStatus = toServerMatchStatus(game.getMatchStatus());

        return new GameStateSnapshot(
            matchId,
            matchStatus,
            currentTurnUserId,
            phase,
            playerSnapshots,
            countrySnapshots,
            continentSnapshots,
            game.getCurrentPlayer()
        );
    }

    public AttackResultDto toAttackResult(AttackResultVO vo) {
        return new AttackResultDto(
            vo.getSrcCountry(),
            vo.getTargetCountry(),
            vo.getAttackers(),
            vo.getDefense(),
            vo.getSrcCountryLoss(),
            vo.getTargetCountryLoss(),
            vo.isConquered(),
            vo.isPlayerDestroyed()
        );
    }

    public String phaseToString(int turnPhase) {
        return switch (turnPhase) {
            case ClassicGameConstants.TURN_PHASE_ADD -> "ADD_TROOPS";
            case ClassicGameConstants.TURN_PHASE_ATTACK -> "ATTACK";
            case ClassicGameConstants.TURN_PHASE_MOVE -> "MOVE_TROOPS";
            default -> "UNKNOWN";
        };
    }

    public MatchStatus toServerMatchStatus(ClassicGame.MatchStatus coreStatus) {
        if (coreStatus == null) {
            return MatchStatus.IN_PROGRESS;
        }
        return switch (coreStatus) {
            case LOBBY -> MatchStatus.LOBBY;
            case IN_PROGRESS -> MatchStatus.IN_PROGRESS;
            case FINISHED -> MatchStatus.FINISHED;
        };
    }

    private UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
