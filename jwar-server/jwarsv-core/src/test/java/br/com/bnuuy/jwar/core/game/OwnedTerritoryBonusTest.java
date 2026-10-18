package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the "owned-territory card bonus" rule.
 *
 * <p>When a player exchanges a trio that includes a card whose
 * territory they currently own, that specific territory gains +2 troops
 * (independently per matching card -- two matching cards = +2 on each
 * of two territories).</p>
 *
 * <p>{@code @Disabled} until spec 002 lands.</p>
 */
@DisplayName("ExchangeCardsEvaluator -- owned-territory +2 bonus")
class OwnedTerritoryBonusTest {

	@Test
	@Disabled("spec 002 -- +2 troops on each owned territory whose card is in the exchange")
	@DisplayName("each matching owned-territory card grants +2 troops on that territory")
	void ownedTerritoryCardGrantsTwoExtraTroops() {
		// TODO(spec 002): exchange a trio containing card C1, where the
		// player owns territory(C1). Assert territory(C1).troops += 2
		// AFTER the standard exchange bonus is applied.
	}

	@Test
	@Disabled("spec 002 -- bonus applies once per matching card, not once per exchange")
	@DisplayName("two matching cards => two distinct +2 bonuses on two territories")
	void bonusPerMatchingCardNotPerExchange() {
		// TODO(spec 002): trio (C1, C2, C3) with the player owning
		// territories(C1) and territories(C2) but not territories(C3).
		// territories(C1).troops += 2 AND territories(C2).troops += 2.
	}
}
