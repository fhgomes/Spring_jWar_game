package br.com.bnuuy.jwar.core.game.utils.objective;

import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;

/**
 * Interface for evaluating if a player has completed their objective.
 */
public interface ObjectiveEvaluator {
    /**
     * Evaluates if the player has completed their objective.
     *
     * @param player the player to evaluate
     * @param objective the player's objective
     * @return true if the player has completed their objective, false otherwise
     */
    boolean hasCompletedObjective(ClassicGamePlayer player, EObjectiveCard objective);
}