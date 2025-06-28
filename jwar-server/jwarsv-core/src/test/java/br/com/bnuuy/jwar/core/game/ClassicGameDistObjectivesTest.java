package br.com.bnuuy.jwar.core.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
import br.com.bnuuy.jwar.core.game.utils.EndGameEvaluator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassicGameDistObjectivesTest {

    @Spy
    ClassicGamePlayer p1 = new ClassicGamePlayer("1a", "fer");

    @Spy
    ClassicGamePlayer p2 = new ClassicGamePlayer("2b", "marc");

    @Spy
    ClassicGamePlayer p3 = new ClassicGamePlayer("3c", "alic");

    @Mock
    EndGameEvaluator endGameEvaluator;

    ClassicGameDist classicGameDist;
    List<EObjectiveCard> objectiveCardsDeck;

    @BeforeEach
    void setUp() {
        classicGameDist = new ClassicGameDist();
        objectiveCardsDeck = new ArrayList<>();
        p1.setPlaySeq(1);
        p2.setPlaySeq(2);
        p3.setPlaySeq(3);
    }

    @Test
    @DisplayName("Should initialize and shuffle objective cards deck")
    void initializeAndShuffleObjectiveCardsDeck() {
        // When
        classicGameDist.initializeAndShuffleObjectiveCardsDeck(objectiveCardsDeck);

        // Then
        assertAll(
            () -> assertEquals(EObjectiveCard.values().length, objectiveCardsDeck.size()),
            () -> assertTrue(containsAllObjectiveCards(objectiveCardsDeck))
        );
    }

    @Test
    @DisplayName("Should distribute objective cards to players")
    void distributeObjectiveCards() {
        // Given
        classicGameDist.initializeAndShuffleObjectiveCardsDeck(objectiveCardsDeck);
        List<ClassicGamePlayer> players = List.of(p1, p2, p3);
        int initialDeckSize = objectiveCardsDeck.size();

        // When
        classicGameDist.distributeObjectiveCards(objectiveCardsDeck, players, endGameEvaluator);

        // Then
        assertAll(
            () -> assertEquals(initialDeckSize - players.size(), objectiveCardsDeck.size()),
            () -> verify(endGameEvaluator, times(1)).assignObjective(eq(1), any(EObjectiveCard.class)),
            () -> verify(endGameEvaluator, times(1)).assignObjective(eq(2), any(EObjectiveCard.class)),
            () -> verify(endGameEvaluator, times(1)).assignObjective(eq(3), any(EObjectiveCard.class))
        );
    }

    @Test
    @DisplayName("Should throw exception when trying to distribute objective cards with empty deck")
    void distributeObjectiveCardsEmptyDeck() {
        // Given
        List<ClassicGamePlayer> players = List.of(p1, p2, p3);

        // When/Then
        assertThrows(GameRulesException.class, () -> {
            classicGameDist.distributeObjectiveCards(objectiveCardsDeck, players, endGameEvaluator);
        });

        // Verify that no objectives were assigned
        verify(endGameEvaluator, times(0)).assignObjective(anyInt(), any(EObjectiveCard.class));
    }

    private boolean containsAllObjectiveCards(List<EObjectiveCard> deck) {
        Set<EObjectiveCard> uniqueCards = new HashSet<>(deck);
        return uniqueCards.size() == EObjectiveCard.values().length;
    }

    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
