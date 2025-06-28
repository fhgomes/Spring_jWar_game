package br.com.bnuuy.jwar.core.game;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGameAttackResProcessor {

	private final Map<Integer, List<ClassicGameContinent>> continentOwners;

	public ClassicGameAttackResProcessor(Map<Integer, List<ClassicGameContinent>> continentOwners) {
		this.continentOwners = continentOwners;
	}

	public AttackResultVO process(AttackResultVO result, ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		implyDmg(result, srcCountry, tgtCountry);
		checkConquer(result, srcCountry, tgtCountry);

		return result;
	}

	private void checkConquer(AttackResultVO result, ClassicGameCountry srcCountry,
									 ClassicGameCountry tgtCountry) {
		if (result.isConquered()) {
			// Change ownership of the country
			tgtCountry.changeOwner(srcCountry.getOwner(), srcCountry.getPlayerColor());

			// Update continent ownership
			ClassicGameContinent continent = tgtCountry.getContinent();
			continent.updateOwnership();

			// Update continentOwners map
			updateContinentOwnership(continent);
		}
	}

	private static void implyDmg(AttackResultVO result, ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		srcCountry.removeTroops(result.getSrcCountry());
		tgtCountry.removeTroops(result.getTargetCountryLoss());
		if (tgtCountry.getTroopsCount() < 1) {
			result.setConquered(true);
		}
	}

	/**
	 * Updates the continentOwners map when a continent's ownership changes
	 */
	private void updateContinentOwnership(ClassicGameContinent continent) {
		// First remove the continent from any player's list
		for (List<ClassicGameContinent> playerContinents : continentOwners.values()) {
			playerContinents.remove(continent);
		}

		// Then add it to the new owner's list if it has an owner
		int ownerCode = continent.getGamePlayerOwner();
		if (ownerCode > 0) {
			if (!continentOwners.containsKey(ownerCode)) {
				continentOwners.put(ownerCode, new ArrayList<>());
			}
			continentOwners.get(ownerCode).add(continent);
		}
	}
}
