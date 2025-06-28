package br.com.bnuuy.jwar.core.game.utils.objective;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicContinents;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.map.EObjectiveType;

import java.util.List;
import java.util.Map;

/**
 * Evaluator for objectives that require conquering specific continents.
 */
public class ContinentObjectiveEvaluator implements ObjectiveEvaluator {

    private final Map<Integer, List<ClassicGameContinent>> continentOwners;

    public ContinentObjectiveEvaluator(Map<Integer, List<ClassicGameContinent>> continentOwners) {
        this.continentOwners = continentOwners;
    }

    @Override
    public boolean hasCompletedObjective(ClassicGamePlayer player, EObjectiveCard objective) {
        if (objective.getType() != EObjectiveType.CONQUER_CONTINENTS &&
            objective.getType() != EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE) {
            return false;
        }

        List<EClassicContinents> requiredContinents = objective.getContinents();
        if (requiredContinents == null || requiredContinents.isEmpty()) {
            return false;
        }

        // Get the continents owned by the player
        List<ClassicGameContinent> ownedContinents = continentOwners.get(player.getPlaySeq());
        if (ownedContinents == null || ownedContinents.isEmpty()) {
            return false;
        }

        // Check if the player owns all the required continents
        for (EClassicContinents requiredContinent : requiredContinents) {
            boolean ownsContinent = false;
            for (ClassicGameContinent ownedContinent : ownedContinents) {
                if (ownedContinent.getContinent() == requiredContinent) {
                    ownsContinent = true;
                    break;
                }
            }
            if (!ownsContinent) {
                return false;
            }
        }

        // If the objective requires an additional continent of the player's choice,
        // check if the player owns at least one more continent than the required ones
        if (objective.getType() == EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE) {
            return ownedContinents.size() > requiredContinents.size();
        }

        // If we're here, the player owns all the required continents
        return true;
    }
}