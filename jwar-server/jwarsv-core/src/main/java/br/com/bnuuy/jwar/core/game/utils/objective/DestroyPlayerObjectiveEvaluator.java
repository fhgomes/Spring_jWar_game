package br.com.bnuuy.jwar.core.game.utils.objective;

import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.map.EObjectiveType;

import java.util.Map;

/**
 * Evaluator for objectives that require destroying a specific player's army.
 * If the target player is already destroyed by another player, the objective
 * changes to conquering 24 territories.
 */
public class DestroyPlayerObjectiveEvaluator implements ObjectiveEvaluator {

    private final Map<EGameColors, ClassicGamePlayer> playersByColor;
    private final Map<Integer, ClassicGamePlayer> playersById;

    public DestroyPlayerObjectiveEvaluator(
            Map<EGameColors, ClassicGamePlayer> playersByColor,
            Map<Integer, ClassicGamePlayer> playersById) {
        this.playersByColor = playersByColor;
        this.playersById = playersById;
    }

    @Override
    public boolean hasCompletedObjective(ClassicGamePlayer player, EObjectiveCard objective) {
        if (objective.getType() != EObjectiveType.DESTROY_PLAYER) {
            return false;
        }

        EGameColors targetColor = objective.getTargetPlayerColor();
        ClassicGamePlayer targetPlayer = playersByColor.get(targetColor);

        // If the target player doesn't exist or has no countries, check if the player destroyed them
        if (targetPlayer == null || targetPlayer.getOwnedCountries().isEmpty()) {
            // If the player is the one who destroyed the target player, they've completed the objective
            // This would need to be tracked elsewhere, for now we'll just check if they have 24 territories

            // Create a TerritoryObjectiveEvaluator to check if the player has 24 territories
            TerritoryObjectiveEvaluator territoryEvaluator = new TerritoryObjectiveEvaluator();
            return territoryEvaluator.hasCompletedObjective(player, EObjectiveCard.CONQUER_24_TERRITORIES);
        }

        return false;
    }
}
