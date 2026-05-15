package br.com.bnuuy.jwar.server.service;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.ClassicGame;
import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.ClassicGameLobby;
import br.com.bnuuy.jwar.core.game.ClassicGamePActions;
import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
import br.com.bnuuy.jwar.server.api.mapper.GameStateMapper;
import br.com.bnuuy.jwar.server.domain.Match;
import br.com.bnuuy.jwar.server.domain.MatchStatus;
import br.com.bnuuy.jwar.server.domain.Room;
import br.com.bnuuy.jwar.server.domain.RoomMember;
import br.com.bnuuy.jwar.server.domain.RoomStatus;
import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.dto.AttackResponse;
import br.com.bnuuy.jwar.server.dto.AttackResultDto;
import br.com.bnuuy.jwar.server.dto.ExchangeCardsResponse;
import br.com.bnuuy.jwar.server.dto.GameEventType;
import br.com.bnuuy.jwar.server.dto.GameStateSnapshot;
import br.com.bnuuy.jwar.server.dto.MatchSummary;
import br.com.bnuuy.jwar.server.exception.BadRequestException;
import br.com.bnuuy.jwar.server.exception.ForbiddenException;
import br.com.bnuuy.jwar.server.exception.NotFoundException;
import br.com.bnuuy.jwar.server.repository.MatchRepository;
import br.com.bnuuy.jwar.server.repository.RoomRepository;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import br.com.bnuuy.jwar.server.ws.GameEventPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final RoomRepository roomRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MatchRegistry matchRegistry;
    private final RoomService roomService;
    private final GameStateMapper gameStateMapper;
    private final GameEventPublisher eventPublisher;

    @Transactional
    public Match startMatch(UUID roomId, UUID actingUserId) {
        Room room = roomService.loadRoom(roomId);
        roomService.ensureHost(room, actingUserId);
        if (room.getStatus() != RoomStatus.OPEN && room.getStatus() != RoomStatus.FULL) {
            throw new BadRequestException("Sala não está apta a iniciar partida", "ROOM_NOT_READY");
        }
        int memberCount = room.getMembers().size();
        if (memberCount < 3) {
            throw new BadRequestException("Partida exige ao menos 3 jogadores", "NOT_ENOUGH_PLAYERS");
        }
        if (memberCount > 6) {
            throw new BadRequestException("Partida não pode ter mais de 6 jogadores", "TOO_MANY_PLAYERS");
        }

        ClassicGame game = new ClassicGame(new ClassicGameDist());
        ClassicGameLobby lobby = new ClassicGameLobby(game);
        Map<UUID, User> users = new HashMap<>();
        for (RoomMember member : room.getMembers()) {
            User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário do membro não encontrado"));
            users.put(user.getId(), user);
            ClassicGamePlayer corePlayer = new ClassicGamePlayer(user.getId().toString(), user.getDisplayName());
            lobby.joinLobby(corePlayer);
        }
        try {
            lobby.startMatch();
        } catch (GameRulesException ex) {
            throw ex;
        }

        Match match = new Match();
        match.setId(game.getMatchId());
        match.setRoomId(roomId);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setStartedAt(Instant.now());
        match.setCurrentPhase(gameStateMapper.phaseToString(game.getTurnPhase()));
        match.setCurrentTurnUserId(resolveCurrentTurnUserId(game));
        Match saved = matchRepository.save(match);

        matchRegistry.register(saved.getId(), game);
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setStartedAt(Instant.now());
        room.setUpdatedAt(Instant.now());
        roomRepository.save(room);

        GameStateSnapshot snapshot = gameStateMapper.toSnapshot(saved.getId(), game);
        eventPublisher.publishRoomEvent(roomId, GameEventType.MATCH_STARTED, snapshot);
        eventPublisher.publishMatchEvent(saved.getId(), GameEventType.MATCH_STARTED, snapshot);
        log.info("Match started id={} room={}", saved.getId(), roomId);
        return saved;
    }

    @Transactional(readOnly = true)
    public GameStateSnapshot getSnapshot(UUID matchId, UUID userId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new NotFoundException("Partida não encontrada"));
        ensureParticipant(match, userId);
        ClassicGame game = matchRegistry.get(matchId);
        return gameStateMapper.toSnapshot(matchId, game);
    }

    @Transactional
    public GameStateSnapshot addTroops(UUID matchId, UUID userId, int countryCode, int qty) {
        return runCommand(matchId, userId, (game, seq) -> {
            ClassicGamePActions actions = new ClassicGamePActions(game);
            if (game.getTurnPhase() != ClassicGameConstants.TURN_PHASE_ADD) {
                throw new GameRulesException("Não é possível adicionar tropas fora da fase de adição");
            }
            actions.addTroops(seq, qty, countryCode);
            return new CommandResult(GameEventType.TROOPS_ADDED, null);
        });
    }

    @Transactional
    public AttackResponse attack(UUID matchId, UUID userId, int srcCountryCode, int tgtCountryCode) {
        UUID resolvedMatchId = matchId;
        ReentrantLock lock = matchRegistry.lockFor(matchId);
        lock.lock();
        try {
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Partida não encontrada"));
            ensureParticipant(match, userId);
            ClassicGame game = requireRegisteredGame(matchId);
            int seq = resolvePlayerSeq(game, userId);
            ClassicGamePActions actions = new ClassicGamePActions(game);
            ClassicGameCountry beforeSrc = game.getCountry(srcCountryCode);
            ClassicGameCountry beforeTgt = game.getCountry(tgtCountryCode);
            int srcTroopsBefore = beforeSrc != null ? beforeSrc.getTroopsCount() : 0;
            int tgtTroopsBefore = beforeTgt != null ? beforeTgt.getTroopsCount() : 0;
            int srcOwnerBefore = beforeSrc != null ? beforeSrc.getOwnerCode() : -1;
            int tgtOwnerBefore = beforeTgt != null ? beforeTgt.getOwnerCode() : -1;

            actions.attack(seq, srcCountryCode, tgtCountryCode);

            ClassicGameCountry afterSrc = game.getCountry(srcCountryCode);
            ClassicGameCountry afterTgt = game.getCountry(tgtCountryCode);
            int srcLoss = srcTroopsBefore - (afterSrc != null ? afterSrc.getTroopsCount() : 0);
            int tgtLoss = tgtTroopsBefore - (afterTgt != null ? afterTgt.getTroopsCount() : 0);
            if (srcLoss < 0) srcLoss = 0;
            if (tgtLoss < 0) tgtLoss = 0;
            boolean conquered = afterTgt != null && afterTgt.getOwnerCode() != tgtOwnerBefore
                && afterTgt.getOwnerCode() == srcOwnerBefore;

            AttackResultVO vo = AttackResultVO.builder()
                .srcCountry(srcCountryCode)
                .targetCountry(tgtCountryCode)
                .attackers(new int[0])
                .defense(new int[0])
                .srcCountryLoss(srcLoss)
                .targetCountryLoss(tgtLoss)
                .conquered(conquered)
                .build();
            AttackResultDto resultDto = gameStateMapper.toAttackResult(vo);
            updateMatchAfter(match, game);
            GameStateSnapshot snapshot = gameStateMapper.toSnapshot(matchId, game);
            eventPublisher.publishMatchEvent(resolvedMatchId, GameEventType.ATTACK_RESULT,
                new AttackResponse(resultDto, snapshot));
            if (game.getMatchStatus() == ClassicGame.MatchStatus.FINISHED) {
                publishMatchFinished(match, game, snapshot);
            }
            return new AttackResponse(resultDto, snapshot);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public GameStateSnapshot moveTroops(UUID matchId, UUID userId, int srcCountryCode, int tgtCountryCode, int qty) {
        return runCommand(matchId, userId, (game, seq) -> {
            ClassicGamePActions actions = new ClassicGamePActions(game);
            actions.moveTroops(seq, srcCountryCode, tgtCountryCode, qty);
            return new CommandResult(GameEventType.TROOPS_MOVED, null);
        });
    }

    @Transactional
    public GameStateSnapshot endPhase(UUID matchId, UUID userId) {
        return runCommand(matchId, userId, (game, seq) -> {
            ClassicGamePActions actions = new ClassicGamePActions(game);
            int phase = game.getTurnPhase();
            GameEventType eventType;
            if (phase == ClassicGameConstants.TURN_PHASE_ADD) {
                actions.endCurrentTurnAddPhase(seq);
                eventType = GameEventType.PHASE_ENDED;
            } else if (phase == ClassicGameConstants.TURN_PHASE_ATTACK) {
                actions.endCurrentTurnAttackPhase(seq);
                eventType = GameEventType.PHASE_ENDED;
            } else {
                actions.endCurrentTurnMovePhase(seq);
                eventType = GameEventType.TURN_CHANGED;
            }
            return new CommandResult(eventType, null);
        });
    }

    @Transactional
    public ExchangeCardsResponse exchangeCards(UUID matchId, UUID userId, List<Integer> cardCodes) {
        ReentrantLock lock = matchRegistry.lockFor(matchId);
        lock.lock();
        try {
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Partida não encontrada"));
            ensureParticipant(match, userId);
            ClassicGame game = requireRegisteredGame(matchId);
            int seq = resolvePlayerSeq(game, userId);
            ClassicGamePlayer playerBefore = game.getPlayer(seq);
            int troopsBefore = playerBefore != null ? playerBefore.getAvailableTroops() : 0;
            new ClassicGamePActions(game).exchangeCards(seq, cardCodes);
            ClassicGamePlayer playerAfter = game.getPlayer(seq);
            int troopsAwarded = (playerAfter != null ? playerAfter.getAvailableTroops() : 0) - troopsBefore;
            updateMatchAfter(match, game);
            GameStateSnapshot snapshot = gameStateMapper.toSnapshot(matchId, game);
            eventPublisher.publishMatchEvent(matchId, GameEventType.CARDS_EXCHANGED, snapshot);
            return new ExchangeCardsResponse(Math.max(0, troopsAwarded), snapshot);
        } finally {
            lock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public List<MatchSummary> listMyMatches(UUID userId) {
        return matchRepository.findByParticipantUserId(userId).stream()
            .map(m -> new MatchSummary(
                m.getId(),
                m.getRoomId(),
                m.getStatus(),
                m.getStartedAt(),
                m.getFinishedAt(),
                m.getWinnerUserId(),
                m.getCurrentTurnUserId(),
                m.getCurrentPhase()
            )).toList();
    }

    /* ----- helpers ----- */

    private GameStateSnapshot runCommand(UUID matchId, UUID userId, CommandFunction fn) {
        ReentrantLock lock = matchRegistry.lockFor(matchId);
        lock.lock();
        try {
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Partida não encontrada"));
            ensureParticipant(match, userId);
            ClassicGame game = requireRegisteredGame(matchId);
            int seq = resolvePlayerSeq(game, userId);
            CommandResult result = fn.apply(game, seq);
            updateMatchAfter(match, game);
            GameStateSnapshot snapshot = gameStateMapper.toSnapshot(matchId, game);
            if (result != null && result.eventType != null) {
                eventPublisher.publishMatchEvent(matchId, result.eventType, snapshot);
            }
            if (game.getMatchStatus() == ClassicGame.MatchStatus.FINISHED) {
                publishMatchFinished(match, game, snapshot);
            }
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

    private void publishMatchFinished(Match match, ClassicGame game, GameStateSnapshot snapshot) {
        match.setStatus(MatchStatus.FINISHED);
        match.setFinishedAt(Instant.now());
        ClassicGamePlayer winner = game.getWinner();
        if (winner != null) {
            try {
                match.setWinnerUserId(UUID.fromString(winner.getUserId()));
            } catch (IllegalArgumentException ignore) {
                // leave null
            }
        }
        matchRepository.save(match);
        Room room = roomRepository.findById(match.getRoomId()).orElse(null);
        if (room != null) {
            room.setStatus(RoomStatus.CLOSED);
            room.setEndedAt(Instant.now());
            room.setUpdatedAt(Instant.now());
            roomRepository.save(room);
        }
        eventPublisher.publishMatchEvent(match.getId(), GameEventType.MATCH_FINISHED, snapshot);
        matchRegistry.remove(match.getId());
    }

    private void updateMatchAfter(Match match, ClassicGame game) {
        match.setCurrentTurnUserId(resolveCurrentTurnUserId(game));
        match.setCurrentPhase(gameStateMapper.phaseToString(game.getTurnPhase()));
        matchRepository.save(match);
    }

    private UUID resolveCurrentTurnUserId(ClassicGame game) {
        ClassicGamePlayer current = game.getPlayer(game.getCurrentPlayer());
        if (current == null) {
            return null;
        }
        try {
            return UUID.fromString(current.getUserId());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int resolvePlayerSeq(ClassicGame game, UUID userId) {
        String target = userId.toString();
        for (int seq = 1; seq <= game.getQtdPlayers(); seq++) {
            ClassicGamePlayer p = game.getPlayer(seq);
            if (p != null && target.equals(p.getUserId())) {
                return seq;
            }
        }
        throw new ForbiddenException("Você não é participante desta partida");
    }

    public ClassicGame requireRegisteredGame(UUID matchId) {
        return Optional.ofNullable(matchRegistry.get(matchId))
            .orElseThrow(() -> new NotFoundException("Partida não está ativa em memória"));
    }

    public void ensureParticipant(Match match, UUID userId) {
        Room room = roomService.loadRoom(match.getRoomId());
        boolean isParticipant = room.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(userId));
        if (!isParticipant) {
            throw new ForbiddenException("Você não é participante desta partida");
        }
    }

    @FunctionalInterface
    private interface CommandFunction {
        CommandResult apply(ClassicGame game, int seq);
    }

    private record CommandResult(GameEventType eventType, Object payload) {}
}
