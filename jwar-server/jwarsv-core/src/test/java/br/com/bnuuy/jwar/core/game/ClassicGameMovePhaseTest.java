package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guards for the MOVE phase of a turn.
 *
 * <p>The classic "War" rule set requires:</p>
 * <ul>
 *   <li>Troops can only be moved between countries the player owns AND
 *       that are connected via a chain of owned countries (contiguous
 *       only).</li>
 *   <li>The source country MUST keep at least 1 troop after the move.</li>
 *   <li>Each troop may participate in at most one move per turn (no
 *       chaining a troop A -> B -> C).</li>
 * </ul>
 *
 * <p>All tests {@code @Disabled} -- to be enabled by the implementing
 * agent for spec 002.</p>
 */
@DisplayName("ClassicGame MOVE phase rules")
class ClassicGameMovePhaseTest {

	@Test
	@Disabled("spec 002 -- MOVE rule: contiguous-owned only")
	@DisplayName("moves between non-contiguous owned countries are rejected")
	void movesRequireContiguousOwnership() {
		// TODO(spec 002): GameRulesException when source/target are
		// not connected via a chain of countries owned by the same
		// player.
	}

	@Test
	@Disabled("spec 002 -- MOVE rule: source keeps >=1 troop")
	@DisplayName("moves that would empty the source country are rejected")
	void sourceMustKeepAtLeastOneTroop() {
		// TODO(spec 002): GameRulesException if moveCount >= src.troops.
	}

	@Test
	@Disabled("spec 002 -- MOVE rule: each troop moves at most once per turn")
	@DisplayName("a troop cannot be moved twice in the same MOVE phase")
	void noChainMoves() {
		// TODO(spec 002): track per-troop (or per-country) "already
		// moved" flag for the current turn.
	}
}
