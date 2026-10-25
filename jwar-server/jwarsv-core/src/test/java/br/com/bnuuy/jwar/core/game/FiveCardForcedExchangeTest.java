package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the "five cards forces an exchange" rule.
 *
 * <p>Classic "War" caps a player's hand at four cards at the start of
 * their turn -- a player who would begin a turn with five MUST exchange
 * before the ADD_TROOPS phase opens. The engine enforces this by
 * refusing to advance the turn until the exchange completes.</p>
 *
 * <p>{@code @Disabled} until spec 002 lands.</p>
 */
@DisplayName("Forced card exchange at turn-start with >=5 cards")
class FiveCardForcedExchangeTest {

	@Test
	@Disabled("spec 002 -- turn cannot start with 5 cards in hand")
	@DisplayName("a player with 5 cards is blocked from ADD_TROOPS until they exchange")
	void turnStartBlockedAtFiveCards() {
		// TODO(spec 002): given a player whose hand has 5 cards,
		// calling startTurn (or addTroops) must throw GameRulesException
		// with a message identifying the forced-exchange requirement.
	}

	@Test
	@Disabled("spec 002 -- 4 cards is the legal maximum at turn start")
	@DisplayName("a player with 4 cards may start their turn normally")
	void fourCardsIsLegal() {
		// TODO(spec 002): boundary -- exactly 4 cards allows turn to
		// proceed without forcing an exchange.
	}
}
