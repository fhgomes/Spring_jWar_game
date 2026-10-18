package br.com.bnuuy.jwar.core.game.utils;

import static java.lang.String.format;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.map.ECardShape;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Evaluator for card exchanges in the Classic War game.
 * Handles the rules for exchanging cards and calculating troop bonuses.
 */
@Slf4j
public class CardExchangeEvaluator {

    private static final String INVALID_EXCHANGE_NOT_ENOUGH_CARDS = "Cannot exchange cards: player needs at least 3 cards but has [%d]";
    private static final String INVALID_EXCHANGE_INVALID_COMBINATION = "Cannot exchange cards: selected cards do not form a valid combination";


    /**
     * Checks if the given cards can be exchanged.
     * Valid combinations are:
     * - 3 cards of the same shape
     * - 3 cards of different shapes
     *
     * @param cards the cards to check
     * @return true if the cards can be exchanged, false otherwise
     */
    public static boolean canExchangeCards(List<EClassicCountryCard> cards) {
        if (cards == null || cards.size() < 3) {
            return false;
        }

        // Group cards by shape
        Map<ECardShape, List<EClassicCountryCard>> cardsByShape = cards.stream()
            .collect(Collectors.groupingBy(EClassicCountryCard::getShape));

        // Check if all cards have the same shape
        if (cardsByShape.size() == 1) {
            return true;
        }

        // Check if all cards have different shapes
        if (cardsByShape.size() == 3) {
            return true;
        }

        return false;
    }

    /**
     * Calculates the number of troops gained from exchanging cards.
     * The number of troops increases with each exchange:
     * - First exchange: 4 troops
     * - Each subsequent exchange: +2 troops until reaching 10
     * - After reaching 10: +5 troops for each exchange
     *
     * @param exchangeCount the number of exchanges made so far (including this one)
     * @return the number of troops gained from this exchange
     */
    public int calculateExchangeTroops(int exchangeCount) {
        if (exchangeCount <= 0) {
            return 0;
        }

        if (exchangeCount == 1) {
            return ClassicGameConstants.INITIAL_EXCHANGE_TROOPS;
        }

        int troops = ClassicGameConstants.INITIAL_EXCHANGE_TROOPS;

        // Calculate troops for exchanges 2 to N
        for (int i = 2; i <= exchangeCount; i++) {
            if (troops < ClassicGameConstants.EXCHANGE_INCREMENT_THRESHOLD) {
                troops += ClassicGameConstants.EXCHANGE_INCREMENT_UNTIL_THRESHOLD;
            } else {
                troops += ClassicGameConstants.EXCHANGE_INCREMENT_AFTER_THRESHOLD;
            }
        }

        return troops;
    }

    /**
     * Validates that the given cards can be exchanged.
     * Throws a GameRulesException if the cards cannot be exchanged.
     *
     * @param cards the cards to validate
     * @throws GameRulesException if the cards cannot be exchanged
     */
    public static void validateExchange(List<EClassicCountryCard> cards) {
        if (cards == null || cards.size() < 3) {
            throw new GameRulesException(format(INVALID_EXCHANGE_NOT_ENOUGH_CARDS, cards == null ? 0 : cards.size()));
        }

        if (!canExchangeCards(cards)) {
            throw new GameRulesException(INVALID_EXCHANGE_INVALID_COMBINATION);
        }
    }

    /**
     * Checks if a player can exchange any combination of their cards.
     * A player can exchange cards if they have at least 3 cards and
     * either 3 cards of the same shape or 3 cards of different shapes.
     *
     * @param playerCards the player's cards
     * @return true if the player can exchange cards, false otherwise
     */
    public static boolean canPlayerExchangeCards(List<EClassicCountryCard> playerCards) {
        if (playerCards == null || playerCards.size() < 3) {
            return false;
        }

        // Check for 3 cards of the same shape
        Map<ECardShape, List<EClassicCountryCard>> cardsByShape = playerCards.stream()
            .collect(Collectors.groupingBy(EClassicCountryCard::getShape));

        for (List<EClassicCountryCard> sameShapeCards : cardsByShape.values()) {
            if (sameShapeCards.size() >= 3) {
                return true;
            }
        }

        // Check for 3 cards of different shapes
        Set<ECardShape> shapes = new HashSet<>();
        for (EClassicCountryCard card : playerCards) {
            shapes.add(card.getShape());
        }

        return shapes.size() >= 3;
    }
}
