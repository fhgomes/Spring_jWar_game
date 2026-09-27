package br.com.bnuuy.jwar.core.game.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExchangeCardsEvaluatorTest {

    @Mock
    private ClassicGameCountry mockCountry;

    @Mock
    private EClassicCountries mockEClassicCountry;

    @Spy
    private ClassicGamePlayer player = new ClassicGamePlayer("1", "TestPlayer");

    @Mock
    private CardExchangeState cardExchangeState;

    private Map<Integer, ClassicGameCountry> countries;
    private List<EClassicCountryCard> cardsDeck;
    private ExchangeCardsEvaluator exchangeCardsEvaluator;

    @BeforeEach
    void setUp() {
        countries = new HashMap<>();
        cardsDeck = new ArrayList<>();

        // Setup mock country
        when(mockCountry.getCountry()).thenReturn(mockEClassicCountry);
        when(mockEClassicCountry.getCode()).thenReturn(1);
        when(mockEClassicCountry.getName()).thenReturn("TestCountry");

        countries.put(1, mockCountry);

        exchangeCardsEvaluator = new ExchangeCardsEvaluator(countries, cardsDeck, cardExchangeState);
    }

    @Test
    @DisplayName("Should validate exchange with less than 3 country codes")
    void validateExchangeWithLessThanThreeCountryCodes() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode()
        );

        // Add the card to the player's hand
        player.addCard(EClassicCountryCard.BRA);

        // When/Then
        assertThrows(GameRulesException.class, () -> {
            ExchangeCardsEvaluator.validateExchange(player, countryCodes);
        });
    }

    @Test
    @DisplayName("Should validate exchange with invalid combination")
    void validateExchangeWithInvalidCombination() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(), // Triangle
            EClassicCountryCard.ARG.getCountry().getCode(), // Circle
            EClassicCountryCard.BRA.getCountry().getCode()  // Triangle (duplicate shape)
        );

        // Add the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.ARG);
        player.addCard(EClassicCountryCard.BRA);

        // When/Then
        assertThrows(GameRulesException.class, () -> {
            ExchangeCardsEvaluator.validateExchange(player, countryCodes);
        });
    }

    @Test
    @DisplayName("Should validate exchange when player doesn't have the card")
    void validateExchangePlayerDoesntHaveCard() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(), // Triangle
            EClassicCountryCard.ARG.getCountry().getCode(), // Circle
            EClassicCountryCard.PER.getCountry().getCode()  // Square
        );

        // Add only some of the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.ARG);
        // Don't add PER

        // When/Then
        assertThrows(GameRulesException.class, () -> {
            ExchangeCardsEvaluator.validateExchange(player, countryCodes);
        });
    }

    @Test
    @DisplayName("Should process first exchange (4 troops)")
    void processFirstExchange() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(), // Triangle
            EClassicCountryCard.ARG.getCountry().getCode(), // Circle
            EClassicCountryCard.PER.getCountry().getCode()  // Square
        );

        // Add the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.ARG);
        player.addCard(EClassicCountryCard.PER);

        when(cardExchangeState.incrementExchangeCount()).thenReturn(4); // First exchange: 4 troops
        when(cardExchangeState.getExchangeCount()).thenReturn(1);

        // When
        int troopsGained = exchangeCardsEvaluator.processCardExchange(player, countryCodes);

        // Then
        assertEquals(4, troopsGained);
        verify(player).addTroops(4);
        assertEquals(3, cardsDeck.size()); // Cards should be returned to the deck
    }

    @Test
    @DisplayName("Should process second exchange (6 troops)")
    void processSecondExchange() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(), // Triangle
            EClassicCountryCard.ARG.getCountry().getCode(), // Circle
            EClassicCountryCard.PER.getCountry().getCode()  // Square
        );

        // Add the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.ARG);
        player.addCard(EClassicCountryCard.PER);

        when(cardExchangeState.incrementExchangeCount()).thenReturn(6); // Second exchange: 6 troops
        when(cardExchangeState.getExchangeCount()).thenReturn(2);

        // When
        int troopsGained = exchangeCardsEvaluator.processCardExchange(player, countryCodes);

        // Then
        assertEquals(6, troopsGained);
        verify(player).addTroops(6);
        assertEquals(3, cardsDeck.size()); // Cards should be returned to the deck
    }

    @Test
    @DisplayName("Should process sixth exchange (15 troops)")
    void processSixthExchange() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(), // Triangle
            EClassicCountryCard.MAC.getCountry().getCode(), // Triangle
            EClassicCountryCard.ALE.getCountry().getCode()  // Triangle
        );

        // Add the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.MAC);
        player.addCard(EClassicCountryCard.ALE);

        when(cardExchangeState.incrementExchangeCount()).thenReturn(15); // Sixth exchange: 15 troops
        when(cardExchangeState.getExchangeCount()).thenReturn(6);

        // When
        int troopsGained = exchangeCardsEvaluator.processCardExchange(player, countryCodes);

        // Then
        assertEquals(15, troopsGained);
        verify(player).addTroops(15);
        assertEquals(3, cardsDeck.size()); // Cards should be returned to the deck
    }

    @Test
    @DisplayName("Should add bonus troops for owned countries")
    void processExchangeWithOwnedCountries() {
        // Given
        // Create a card for the country the player owns
        EClassicCountryCard ownedCard = EClassicCountryCard.BRA;

        List<Integer> countryCodes = List.of(
            ownedCard.getCountry().getCode(),
            EClassicCountryCard.ARG.getCountry().getCode(),
            EClassicCountryCard.PER.getCountry().getCode()
        );

        // Add the cards to the player's hand
        player.addCard(ownedCard);
        player.addCard(EClassicCountryCard.ARG);
        player.addCard(EClassicCountryCard.PER);

        // Setup the country to be owned by the player
        when(mockCountry.getOwner()).thenReturn(player);
        when(mockEClassicCountry.getCode()).thenReturn(ownedCard.getCountry().getCode());

        when(cardExchangeState.incrementExchangeCount()).thenReturn(4);
        when(cardExchangeState.getExchangeCount()).thenReturn(1);

        // When
        int troopsGained = exchangeCardsEvaluator.processCardExchange(player, countryCodes);

        // Then
        assertEquals(4, troopsGained);
        verify(player).addTroops(4);
        verify(mockCountry).addTroops(ClassicGameConstants.COUNTRY_BONUS_TROOPS);
        assertEquals(3, cardsDeck.size()); // Cards should be returned to the deck
    }

    @Test
    @DisplayName("Should not add bonus troops for countries not owned by the player")
    void processExchangeWithNonOwnedCountries() {
        // Given
        List<Integer> countryCodes = List.of(
            EClassicCountryCard.BRA.getCountry().getCode(),
            EClassicCountryCard.ARG.getCountry().getCode(),
            EClassicCountryCard.PER.getCountry().getCode()
        );

        // Add the cards to the player's hand
        player.addCard(EClassicCountryCard.BRA);
        player.addCard(EClassicCountryCard.ARG);
        player.addCard(EClassicCountryCard.PER);

        // Setup the country to not be owned by the player
        when(mockCountry.getOwner()).thenReturn(null);

        when(cardExchangeState.incrementExchangeCount()).thenReturn(4);
        when(cardExchangeState.getExchangeCount()).thenReturn(1);

        // When
        int troopsGained = exchangeCardsEvaluator.processCardExchange(player, countryCodes);

        // Then
        assertEquals(4, troopsGained);
        verify(player).addTroops(4);
        verify(mockCountry, never()).addTroops(anyInt());
        assertEquals(3, cardsDeck.size()); // Cards should be returned to the deck
    }
}
