package br.com.bnuuy.jwar.core.game.map;

/**
 * Represents the types of objectives in the Classic War game.
 */
public enum EObjectiveType {
    /**
     * Destroy a specific player's army.
     * If the target player is already destroyed by another player,
     * the objective changes to CONQUER_TERRITORIES with 24 territories.
     */
    DESTROY_PLAYER,

    /**
     * Conquer specific continents.
     */
    CONQUER_CONTINENTS,

    /**
     * Conquer specific continents plus one continent of the player's choice.
     */
    CONQUER_CONTINENTS_PLUS_ONE,

    /**
     * Conquer a specific number of territories.
     */
    CONQUER_TERRITORIES,

    /**
     * Conquer a specific number of territories with a minimum number of troops in each.
     */
    CONQUER_TERRITORIES_WITH_TROOPS
}