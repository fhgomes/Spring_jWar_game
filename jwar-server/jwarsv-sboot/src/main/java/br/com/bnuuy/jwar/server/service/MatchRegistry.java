package br.com.bnuuy.jwar.server.service;

import br.com.bnuuy.jwar.core.game.ClassicGame;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Holds the in-memory ClassicGame instances keyed by match id. Provides per-match
 * locks to serialise concurrent gameplay commands targeting the same match.
 *
 * <p>State here is bound to the JVM lifecycle — Spec 005 / Spec 006 acknowledge that
 * a restart drops every active match.
 */
@Component
public class MatchRegistry {

    private final ConcurrentMap<UUID, ClassicGame> games = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void register(UUID matchId, ClassicGame game) {
        games.put(matchId, game);
        locks.computeIfAbsent(matchId, k -> new ReentrantLock());
    }

    public ClassicGame get(UUID matchId) {
        return games.get(matchId);
    }

    public Optional<ClassicGame> find(UUID matchId) {
        return Optional.ofNullable(games.get(matchId));
    }

    public void remove(UUID matchId) {
        games.remove(matchId);
        locks.remove(matchId);
    }

    public ReentrantLock lockFor(UUID matchId) {
        return locks.computeIfAbsent(matchId, k -> new ReentrantLock());
    }
}
