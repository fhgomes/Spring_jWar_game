package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guards for end-game transitions.
 *
 * <p>The match's status enum (e.g. {@code IN_PROGRESS},
 * {@code FINISHED}) MUST transition exactly once, when a player
 * completes their secret objective. The winner MUST be recorded on the
 * match snapshot.</p>
 *
 * <p>{@code @Disabled} until spec 002 lands. See
 * {@code ClassicGameAttackResProcessorTest.EndGame} for the
 * attack-driven path; this class covers the more general lifecycle
 * (objective evaluated after move/end-turn too).</p>
 */
@DisplayName("ClassicGame -- end-game lifecycle")
class EndGameTest {

	@Test
	@Disabled("spec 002 -- match transitions to FINISHED with winner set")
	@DisplayName("MatchStatus flips to FINISHED and winner is recorded on objective completion")
	void matchStatusTransitionsOnWin() {
		// TODO(spec 002): given a state in which a player's objective
		// is satisfied:
		//   - game.getStatus() == FINISHED
		//   - game.getWinner() == the player who completed it
		//   - subsequent action attempts throw GameRulesException
	}

	@Test
	@Disabled("spec 002 -- last-player-standing also ends the game")
	@DisplayName("a single survivor ends the match even without objective evaluation")
	void lastPlayerStandingEndsMatch() {
		// TODO(spec 002): eliminating every opponent must FINISH the
		// match with the survivor as the winner.
	}
}
