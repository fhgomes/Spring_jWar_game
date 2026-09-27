package br.com.bnuuy.jwar.core.game.utils;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.map.EObjectiveType;
import br.com.bnuuy.jwar.core.game.utils.objective.ContinentObjectiveEvaluator;
import br.com.bnuuy.jwar.core.game.utils.objective.DestroyPlayerObjectiveEvaluator;
import br.com.bnuuy.jwar.core.game.utils.objective.ObjectiveEvaluator;
import br.com.bnuuy.jwar.core.game.utils.objective.TerritoryObjectiveEvaluator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates if a player has won the game based on their objective.
 */
public class EndGameEvaluator {

    private final Map<EObjectiveType, ObjectiveEvaluator> evaluators;

    /**
     * Creates a new EndGameEvaluator.
     *
     * @param continentOwners map of player IDs to the continents they own
     * @param playersByColor map of player colors to player objects
     * @param playersById map of player IDs to player objects
     */
    public EndGameEvaluator(
            Map<Integer, List<ClassicGameContinent>> continentOwners,
            Map<EGameColors, ClassicGamePlayer> playersByColor,
            Map<Integer, ClassicGamePlayer> playersById) {

        this.evaluators = new HashMap<>();

        // Initialize the evaluators
        evaluators.put(EObjectiveType.CONQUER_TERRITORIES, new TerritoryObjectiveEvaluator());
        evaluators.put(EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS, new TerritoryObjectiveEvaluator());
        evaluators.put(EObjectiveType.CONQUER_CONTINENTS, new ContinentObjectiveEvaluator(continentOwners));
        evaluators.put(EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE, new ContinentObjectiveEvaluator(continentOwners));
        evaluators.put(EObjectiveType.DESTROY_PLAYER, new DestroyPlayerObjectiveEvaluator(playersByColor, playersById));
    }

    /**
     * Checks if a player has completed their objective.
     *
     * @param player the player to check
     * @return true if the player has completed their objective, false otherwise
     */
    public boolean hasPlayerWon(ClassicGamePlayer player) {
        EObjectiveCard objective = player.getGameObjective();
        if (objective == null) {
            return false;
        }

        ObjectiveEvaluator evaluator = evaluators.get(objective.getType());
        if (evaluator == null) {
            return false;
        }

        return evaluator.hasCompletedObjective(player, objective);
    }
}