package br.com.bnuuy.jwar.core.game;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassicGameTest {

	@Spy
	ClassicGamePlayer p1 = new ClassicGamePlayer("1a", "fer");

	@Spy
	ClassicGamePlayer p2 = new ClassicGamePlayer("2b", "marc");

	@Spy
	ClassicGamePlayer p3 = new ClassicGamePlayer("3c", "alic");

	@Spy
	ClassicGamePlayer p4 = new ClassicGamePlayer("4d", "dav");

	@Spy
	ClassicGamePlayer p5 = new ClassicGamePlayer("5f", "pita");

	@Spy
	ClassicGamePlayer p6 = new ClassicGamePlayer("6g", "woken");

	@Spy
	ClassicGamePlayer p7 = new ClassicGamePlayer("7h", "seila");

	ClassicGame classicGame;

	@BeforeEach
	void setUp() {
		ShufflerUtil shufflerUtil = new ShufflerUtil();
		ClassicGameDist classicGameDist = new ClassicGameDist(shufflerUtil);
		ClassicGameValidator classicGameValidator = new ClassicGameValidator();
		classicGame = new ClassicGame(classicGameDist, classicGameValidator);
	}

	@Test
	@DisplayName("Should not allow game - min players")
	void shouldNotStartGameWithMinPlayers() {
		assertThrows(
			GameRulesException.class,
			() -> classicGame.startMatch(List.of(p1, p2))
		);
	}

	@Test
	@DisplayName("Should not allow game - max players")
	void shouldNotStartGameWithMaxPlayers() {
		assertThrows(
			GameRulesException.class,
			() -> classicGame.startMatch(List.of(p1, p2, p3, p4, p5, p6, p7))
		);
	}

	@Test
	@DisplayName("Should not allow game - repeated players")
	void shouldNotStartGameWithRepeatedPlayers() {
		assertThrows(
			GameRulesException.class,
			() -> classicGame.startMatch(List.of(p1, p2, p2, p4, p5, p6))
		);
	}
}