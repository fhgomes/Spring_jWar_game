package br.com.bnuuy.jwar.core.game.utils.objective;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.map.EObjectiveType;

import java.util.List;

/**
 * Evaluator for objectives that require conquering a specific number of territories,
 * optionally with a minimum number of troops in each.
 */
public class TerritoryObjectiveEvaluator implements ObjectiveEvaluator {

    @Override
    public boolean hasCompletedObjective(ClassicGamePlayer player, EObjectiveCard objective) {
        if (objective.getType() != EObjectiveType.CONQUER_TERRITORIES &&
            objective.getType() != EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS) {
            return false;
        }

        List<ClassicGameCountry> ownedCountries = player.getOwnedCountries();
        int territoryCount = objective.getTerritoryCount();
        int minTroopsPerTerritory = objective.getMinTroopsPerTerritory();

        // Check if the player has enough territories
        if (ownedCountries.size() < territoryCount) {
            return false;
        }

        // If the objective requires a minimum number of troops per territory,
        // check if all territories have at least that many troops
        if (objective.getType() == EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS) {
            long countriesWithEnoughTroops = ownedCountries.stream()
                .filter(country -> country.getTroopsCount() >= minTroopsPerTerritory)
                .count();

            return countriesWithEnoughTroops >= territoryCount;
        }

        // If we're here, the player has enough territories and the objective doesn't
        // require a minimum number of troops per territory
        return true;
    }
}