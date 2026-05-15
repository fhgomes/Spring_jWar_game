package br.com.bnuuy.jwar.core.game;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

/**
 * Regression guards for the battle-dice rules in
 * {@code ClassicGameAttacker}, expressing the acceptance criteria of
 * spec 002 (Fix-and-complete game mechanics).
 *
 * <p>Every test in this class is intentionally {@code @Disabled} for
 * now. The implementing agent will:</p>
 * <ol>
 *   <li>Remove the {@code @Disabled} annotation.</li>
 *   <li>Construct a deterministic {@code ClassicGameAttacker} (mocking
 *       the dice RNG so outcomes are reproducible).</li>
 *   <li>Assert against the resulting {@code AttackResultVO} fields.</li>
 * </ol>
 *
 * <p>These tests MUST fail before the spec-002 fix and pass after --
 * that is what makes them regression guards
 * (cf. spec 012 FR-011, SC-004).</p>
 */
@DisplayName("ClassicGameAttacker -- battle dice rules")
class ClassicGameAttackerTest {

	@Nested
	@DisplayName("dice tie")
	class DiceTie {

		@Test
		@Disabled("spec 002 -- battle dice: ties go to the defender")
		@DisplayName("a tie on the highest pair lets the attacker lose a troop")
		void tieFavoursDefender() {
			// TODO(spec 002): given attacker rolls [6,5,2] and defender
			// rolls [6,5], the attacker MUST lose 2 troops and the
			// defender MUST lose 0 (each tied pair counts as a defender
			// win, not a no-op).
		}
	}

	@Nested
	@DisplayName("defender losses")
	class DefenderLosses {

		@Test
		@Disabled("spec 002 -- defender losses bounded by defender dice count")
		@DisplayName("defender losses never exceed min(attacker dice, defender dice)")
		void defenderLossesBoundedByDiceCount() {
			// TODO(spec 002): with attacker rolling 3 dice and defender
			// rolling 1, the defender can lose at MOST 1 troop in this
			// resolution, regardless of attacker roll values.
		}
	}

	@Nested
	@DisplayName("defender dice count")
	class DefenderDiceCount {

		@Test
		@Disabled("spec 002 -- defender dice are based on target country, not source")
		@DisplayName("defender rolls min(3, defender troops) -- never tied to attacker")
		void defenderDiceBasedOnTarget() {
			// TODO(spec 002): a target with 2 troops rolls 2 dice even
			// if the attacking source has 4 troops. The bug being
			// guarded: defender dice incorrectly computed off srcCountry.
		}
	}

	@Nested
	@DisplayName("array bounds")
	class ArrayBounds {

		@Test
		@Disabled("spec 002 -- no ArrayIndexOutOfBounds when defender has fewer dice")
		@DisplayName("attacker rolls 3, defender rolls 1 -- no exception")
		void noArrayOOBWhenDefenderHasFewerDice() {
			// TODO(spec 002): exercise the asymmetric-dice path. Before
			// the fix the comparison loop walked off the defender array.
		}
	}
}
