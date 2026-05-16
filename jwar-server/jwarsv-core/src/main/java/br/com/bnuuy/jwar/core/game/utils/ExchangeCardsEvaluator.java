package br.com.bnuuy.jwar.core.game.utils;

import static java.lang.String.format;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.ECardShape;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Evaluator for card exchanges in the Classic War game.
 * Handles the rules for exchanging cards, calculating troop bonuses,
 * and processing card exchanges.
 */
@Slf4j
public class ExchangeCardsEvaluator {

    private static final String INVALID_EXCHANGE_NOT_ENOUGH_CARDS = "Cannot exchange cards: player needs at least 3 cards but has [%d]";
    private static final String INVALID_EXCHANGE_INVALID_COMBINATION = "Cannot exchange cards: selected cards do not form a valid combination";
    private static final String CARDS_RETURNED_TO_DECK = "Returned [%d] cards to the deck and shuffled.";
    private static final String PLAYER_EXCHANGED_CARDS = "Player [%s] exchanged [%d] cards for [%d] troops (exchange #[%d]).";
    private static final String PLAYER_BONUS_TROOPS = "Player [%s] received [%d] bonus troops for owning country [%s] from exchanged card";

    private final Map<Integer, ClassicGameCountry> countries;
    private final List<EClassicCountryCard> cardsDeck;
    private final CardExchangeState cardExchangeState;

    /**
     * Creates a new ExchangeCardsEvaluator.
     *
     * @param countries the map of countries in the game
     * @param cardsDeck the deck to return the cards to
     * @param cardExchangeState the state object tracking exchange count and prize
     */
    public ExchangeCardsEvaluator(Map<Integer, ClassicGameCountry> countries,
                                 List<EClassicCountryCard> cardsDeck,
                                 CardExchangeState cardExchangeState) {
        this.countries = countries;
        this.cardsDeck = cardsDeck;
        this.cardExchangeState = cardExchangeState;
    }

    /**
     * Processes a card exchange for the specified player.
     * Adds troops to the player based on the current exchange prize,
     * adds bonus troops for countries owned by the player,
     * removes the cards from the player's hand, and returns them to the deck.
     *
     * @param player the player exchanging cards
     * @param countryCardCodes the card codes to exchange
     * @return the number of troops gained from the exchange
     */
    public int processCardExchange(ClassicGamePlayer player, List<Integer> countryCardCodes) {
        // Get the current prize and increment the exchange count
        int troopsGained = cardExchangeState.incrementExchangeCount();

        // Add troops to the player
        player.addTroops(troopsGained);

		List<EClassicCountryCard> exchangedCards = new ArrayList<>();
		for (Integer countryCode : countryCardCodes) {
			EClassicCountryCard card = EClassicCountryCard.getByCountryCode(countryCode);
			exchangedCards.add(card);

            EClassicCountries cardCountry = card.getCountry();
            ClassicGameCountry gameCountry = countries.get(cardCountry.getCode());

            // If the player owns the country on the card, add bonus troops to that country
            if (gameCountry != null && gameCountry.getOwner() == player) {
                gameCountry.addTroops(ClassicGameConstants.COUNTRY_BONUS_TROOPS);
                log.info(format(PLAYER_BONUS_TROOPS, player.getNickName(),
                    ClassicGameConstants.COUNTRY_BONUS_TROOPS, gameCountry.getCountry().getName()));
            }
        }

        // Remove the cards from the player's hand
        player.removeExchangedCards(exchangedCards);

        // Return the cards to the deck and shuffle
        returnCardsToDeckAndShuffle(exchangedCards);

        log.info(format(PLAYER_EXCHANGED_CARDS, player.getNickName(),
            exchangedCards.size(), troopsGained, cardExchangeState.getExchangeCount()));

        return troopsGained;
    }

    /**
     * Returns the specified cards to the deck and shuffles the deck.
     *
     * @param cards the cards to return to the deck
     */
    private void returnCardsToDeckAndShuffle(List<EClassicCountryCard> cards) {
        if (cards != null && !cards.isEmpty()) {
            cardsDeck.addAll(cards);
            java.util.Collections.shuffle(cardsDeck);
            log.info(format(CARDS_RETURNED_TO_DECK, cards.size()));
        }
    }

    /**
     * Checks if the given cards can be exchanged.
     * Valid combinations are:
     * - 3 cards of the same shape
     * - 3 cards of different shapes
     *
     * @param cards the cards to check
     * @return true if the cards can be exchanged, false otherwise
     */
    public static boolean playerExchangeAvailable(List<EClassicCountryCard> cards) {
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
     * Validates that the given cards can be exchanged.
     * Throws a GameRulesException if the cards cannot be exchanged.
     *
     * @param player the cards to validate
     * @param countryCodes the cards codes to validate
     * @throws GameRulesException if the cards cannot be exchanged
     */
    public static void validateExchange(ClassicGamePlayer player, List<Integer> countryCodes) {
		// Validate country codes
		if (countryCodes == null || countryCodes.size() < 3) {
			throw new GameRulesException("Card exchange requires at least 3 cards");
		}

		List<EClassicCountryCard> cards = new ArrayList<>();
		for (Integer countryCode : countryCodes) {
			// Get the card for this country code
			EClassicCountryCard card = EClassicCountryCard.getByCountryCode(countryCode);

			// Check if the player has this card
			if (!player.getCards().contains(card)) {
				throw new GameRulesException("Player does not have the card for country code: " + countryCode);
			}

			cards.add(card);
		}

		if (cards.size() < 3) {
            throw new GameRulesException(format(INVALID_EXCHANGE_NOT_ENOUGH_CARDS, cards.size()));
        }

        if (!playerExchangeAvailable(cards)) {
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