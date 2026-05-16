package br.com.bnuuy.jwar.core.game.map;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the objective cards in the Classic War game.
 * Each objective card has a description and a type.
 */
@Getter
public enum EObjectiveCard {
    // Destroy a specific player's army
    DESTROY_PLAYER_RED("Destruir totalmente o exército VERMELHO", EObjectiveType.DESTROY_PLAYER, EGameColors.RED),
    DESTROY_PLAYER_BLUE("Destruir totalmente o exército AZUL", EObjectiveType.DESTROY_PLAYER, EGameColors.BLUE),
    DESTROY_PLAYER_GREEN("Destruir totalmente o exército VERDE", EObjectiveType.DESTROY_PLAYER, EGameColors.GREEN),
    DESTROY_PLAYER_YELLOW("Destruir totalmente o exército AMARELO", EObjectiveType.DESTROY_PLAYER, EGameColors.YELLOW),
    DESTROY_PLAYER_PURPLE("Destruir totalmente o exército ROXO", EObjectiveType.DESTROY_PLAYER, EGameColors.PURPLE),
    DESTROY_PLAYER_GRAY("Destruir totalmente o exército CINZA", EObjectiveType.DESTROY_PLAYER, EGameColors.GRAY),

    // Conquer specific continents
    CONQUER_OCEANIA_ASIA("Conquistar a Oceania e a Ásia", EObjectiveType.CONQUER_CONTINENTS,
        Arrays.asList(EClassicContinents.OCE, EClassicContinents.ASI)),
    CONQUER_EUROPE_SOUTH_AMERICA_ANY("Conquistar a Europa, a América do Sul e mais um continente à sua escolha",
        EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE,
        Arrays.asList(EClassicContinents.EUR, EClassicContinents.AMS)),
    CONQUER_NORTH_AMERICA_AFRICA("Conquistar a América do Norte e a África", EObjectiveType.CONQUER_CONTINENTS,
        Arrays.asList(EClassicContinents.AMN, EClassicContinents.AFR)),
    CONQUER_EUROPE_AFRICA_ANY("Conquistar a Europa, a África e mais um continente à sua escolha",
        EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE,
        Arrays.asList(EClassicContinents.EUR, EClassicContinents.AFR)),
    CONQUER_ASIA_SOUTH_AMERICA("Conquistar a Ásia e a América do Sul", EObjectiveType.CONQUER_CONTINENTS,
        Arrays.asList(EClassicContinents.ASI, EClassicContinents.AMS)),
    CONQUER_NORTH_AMERICA_OCEANIA("Conquistar a América do Norte e a Oceania", EObjectiveType.CONQUER_CONTINENTS,
        Arrays.asList(EClassicContinents.AMN, EClassicContinents.OCE)),
    CONQUER_EUROPE_NORTH_AMERICA("Conquistar a Europa e a América do Norte", EObjectiveType.CONQUER_CONTINENTS,
        Arrays.asList(EClassicContinents.EUR, EClassicContinents.AMN)),

    // Conquer territories with minimum troops
    CONQUER_18_TERRITORIES_2_TROOPS("Conquistar 18 territórios com 2 exércitos em cada um",
        EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS, 18, 2),
    CONQUER_16_TERRITORIES_3_TROOPS("Conquistar 16 territórios com 3 exércitos em cada um",
        EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS, 16, 3),

    // Conquer a specific number of territories
    CONQUER_24_TERRITORIES("Conquistar 24 territórios", EObjectiveType.CONQUER_TERRITORIES, 24, 1);

    private final String description;
    private final EObjectiveType type;
    private final List<EClassicContinents> continents;
    private final EGameColors targetPlayerColor;
    private final int territoryCount;
    private final int minTroopsPerTerritory;

    /**
     * Constructor for DESTROY_PLAYER objectives
     */
    EObjectiveCard(String description, EObjectiveType type, EGameColors targetPlayerColor) {
        this.description = description;
        this.type = type;
        this.targetPlayerColor = targetPlayerColor;
        this.continents = null;
        this.territoryCount = 0;
        this.minTroopsPerTerritory = 0;
    }

    /**
     * Constructor for CONQUER_CONTINENTS and CONQUER_CONTINENTS_PLUS_ONE objectives
     */
    EObjectiveCard(String description, EObjectiveType type, List<EClassicContinents> continents) {
        this.description = description;
        this.type = type;
        this.continents = continents;
        this.targetPlayerColor = null;
        this.territoryCount = 0;
        this.minTroopsPerTerritory = 0;
    }

    /**
     * Constructor for CONQUER_TERRITORIES and CONQUER_TERRITORIES_WITH_TROOPS objectives
     */
    EObjectiveCard(String description, EObjectiveType type, int territoryCount, int minTroopsPerTerritory) {
        this.description = description;
        this.type = type;
        this.territoryCount = territoryCount;
        this.minTroopsPerTerritory = minTroopsPerTerritory;
        this.continents = null;
        this.targetPlayerColor = null;
    }

    /**
     * Gets a random objective card.
     *
     * @return a random objective card
     */
    public static EObjectiveCard getRandom() {
        EObjectiveCard[] cards = values();
        int index = (int) (Math.random() * cards.length);
        return cards[index];
    }
}
