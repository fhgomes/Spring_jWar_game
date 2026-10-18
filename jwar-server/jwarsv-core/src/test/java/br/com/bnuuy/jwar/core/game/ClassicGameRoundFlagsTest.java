package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the round-counter / round-flag handling.
 *
 * <p>The classic "War" rules grant special troop-distribution behaviour
 * for the first two rounds. Spec 002 calls out a bug where round 2 did
 * not fire after round 1 completed -- the round counter was never
 * incremented at the right point of the lifecycle.</p>
 *
 * <p>{@code @Disabled} until spec 002 lands.</p>
 */
@DisplayName("ClassicGame -- round/turn lifecycle flags")
class ClassicGameRoundFlagsTest {

	@Test
	@Disabled("spec 002 -- round 2 must fire after round 1 completes")
	@DisplayName("game.round == 2 once every player has completed round 1")
	void roundTwoFiresAfterRoundOne() {
		// TODO(spec 002): play through one complete round (each player
		// takes a turn), then assert game.getRound() == 2 and the
		// distribution rules for round 2 are in effect.
	}

	@Test
	@Disabled("spec 002 -- first-two-rounds special distribution rules")
	@DisplayName("rounds 1 and 2 follow the special distribution rules; round 3 does not")
	void firstTwoRoundsUseSpecialDistribution() {
		// TODO(spec 002): assert the troop-count given to the player
		// at the start of rounds 1 and 2 matches the special rules,
		// and is back to the standard formula at round 3.
	}
}
