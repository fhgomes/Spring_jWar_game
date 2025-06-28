package br.com.bnuuy.jwar.core.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	ClassicGameDist classicGameDist;

	@BeforeEach
	void setUp() {
		classicGameDist = new ClassicGameDist();
		p1.setPlaySeq(1);
		p2.setPlaySeq(2);
		p3.setPlaySeq(3);
	}

	@Test
	@DisplayName("Should distribute objective cards to players")
	void distributeObjectiveCards() {
		// Given
		List<ClassicGamePlayer> players = List.of(p1, p2, p3);
		classicGameDist.distributeObjectiveCards(players);

		// When
		classicGameDist.distributeObjectiveCards(players);

		// Then
		assertAll(
			() -> verify(p1, times(1)).assignObjective(any(EObjectiveCard.class)),
			() -> verify(p2, times(1)).assignObjective(any(EObjectiveCard.class)),
			() -> verify(p3, times(1)).assignObjective(any(EObjectiveCard.class))
		);
	}

	@Test
	@DisplayName("Should throw exception when trying to distribute objective cards with empty deck")
	void distributeObjectiveCardsEmptyDeck() {
		// Given
		List<ClassicGamePlayer> players = List.of(p1, p2, p3);

		// When/Then
		assertThrows(GameRulesException.class, () -> classicGameDist.distributeObjectiveCards(players));

		// Then
		assertAll(
			() -> verify(p1, never()).assignObjective(any(EObjectiveCard.class)),
			() -> verify(p2, never()).assignObjective(any(EObjectiveCard.class)),
			() -> verify(p3, never()).assignObjective(any(EObjectiveCard.class))
		);
	}

}
