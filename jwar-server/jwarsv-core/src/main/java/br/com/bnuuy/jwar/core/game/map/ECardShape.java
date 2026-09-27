package br.com.bnuuy.jwar.core.game.map;

/**
 * Represents the shapes that can appear on country cards in the Classic War game.
 */
public enum ECardShape {
    TRIANGLE("Triângulo"),
    CIRCLE("Círculo"),
    SQUARE("Quadrado");

    private final String description;

    ECardShape(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}