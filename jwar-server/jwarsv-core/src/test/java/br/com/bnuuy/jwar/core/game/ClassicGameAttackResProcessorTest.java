package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

/**
 * Regression guards for {@code ClassicGameAttackResProcessor} -- the
 * stage that runs <em>after</em> dice are resolved and applies the
 * outcome to the game state (troop deltas, conquests, card transfers,
 * player elimination, end-game evaluation).
 *
 * <p>All tests are {@code @Disabled} stubs -- to be filled in once
 * spec 002 lands. They express the spec's acceptance criteria so
 * reverting any fix surfaces a failing regression guard
 * (spec 012 FR-011, SC-004).</p>
 */
@DisplayName("ClassicGameAttackResProcessor -- post-dice resolution")
class ClassicGameAttackResProcessorTest {

	@Nested
	@DisplayName("conquest")
	class Conquest {

		@Test
		@Disabled("spec 002 -- conquest transfers between 1 and lastAttackDiceCount troops")
		@DisplayName("conquering moves 1..N troops (N == last attack dice count)")
		void conquestTransfersBoundedTroops() {
			// TODO(spec 002): after wiping the defender, the player
			// MUST be required to move at least 1 troop and at most the
			// number of dice rolled on the winning attack.
		}

		@Test
		@Disabled("spec 002 -- source country decrement after conquest")
		@DisplayName("source country's troop count decreases by the moved amount")
		void sourceTroopsDecremented() {
			// TODO(spec 002): srcCountry.troops -= moved.
		}

		@Test
		@Disabled("spec 002 -- conquered country left with >=1 troop")
		@DisplayName("conquered country ends with at least 1 troop")
		void conqueredCountryKeepsAtLeastOneTroop() {
			// TODO(spec 002): tgtCountry.troops >= 1 after conquest.
		}
	}

	@Nested
	@DisplayName("player elimination")
	class PlayerElimination {

		@Test
		@Disabled("spec 002 -- eliminating a player transfers their cards to the attacker")
		@DisplayName("attacker inherits the eliminated player's hand")
		void cardsTransferOnElimination() {
			// TODO(spec 002): when attacker captures the eliminated
			// player's last country, attacker's hand += victim's hand.
		}
	}

	@Nested
	@DisplayName("end game")
	class EndGame {

		@Test
		@Disabled("spec 002 -- objective completion ends the match")
		@DisplayName("match transitions to FINISHED when winner's objective is met")
		void matchEndsOnObjectiveMet() {
			// TODO(spec 002): the post-attack evaluator must flip the
			// match status to FINISHED and record the winning player.
		}
	}
}
