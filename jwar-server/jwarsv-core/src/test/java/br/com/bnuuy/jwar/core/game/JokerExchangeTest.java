package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for joker behaviour in card exchanges.
 *
 * <p>In the classic "War" deck, two jokers act as wild cards that may
 * substitute for any shape (circle, triangle, square) when exchanging
 * a valid trio for bonus troops.</p>
 *
 * <p>{@code @Disabled} until spec 002 lands; the existing
 * {@code ExchangeCardsEvaluatorTest} in this module covers the
 * non-joker happy paths.</p>
 */
@DisplayName("ExchangeCardsEvaluator -- joker rules")
class JokerExchangeTest {

	@Test
	@Disabled("spec 002 -- jokers substitute for any shape")
	@DisplayName("a joker + circle + triangle counts as three-of-a-kind for the missing shape")
	void jokerSubstitutesForMissingShape() {
		// TODO(spec 002): exchange (joker, circle, triangle) must be
		// valid and award the bonus appropriate to whichever shape the
		// joker completes (i.e. the engine picks the most generous
		// matching combination per the classic rules).
	}

	@Test
	@Disabled("spec 002 -- two jokers are valid in a trio")
	@DisplayName("a trio of (joker, joker, square) is a valid exchange")
	void twoJokersAndOneRealCardExchangeable() {
		// TODO(spec 002): jokers count as the missing shape; the
		// resulting exchange is valid and rewards the player.
	}
}
