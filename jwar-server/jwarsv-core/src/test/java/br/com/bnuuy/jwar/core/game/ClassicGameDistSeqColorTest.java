package br.com.bnuuy.jwar.core.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassicGameDistSeqColorTest {

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

	ClassicGameDist classicGameDist;
	Map<Integer, ClassicGamePlayer> gamePlayers;

	@BeforeEach
	void setUp() {
		classicGameDist = new ClassicGameDist();
		gamePlayers = new HashMap<>();
	}

	@Test
	@DisplayName("Should check all sequences distributed by 3")
	void distributeSeq3() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3);
		classicGameDist.distributeSeq(gamePlayers, players);

		int maxPlayersSeq = 3;
		assertAll(
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p1)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p2)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p3)),
			() -> assertTrue(allUniquePlaySeq(players))
		);
	}

	@Test
	@DisplayName("Should check all sequences distributed by 6")
	void distributeSeq6() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3, p4, p5, p6);
		classicGameDist.distributeSeq(gamePlayers, players);

		int maxPlayersSeq = 6;
		assertAll(
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p1)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p2)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p3)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p4)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p5)),
			() -> assertTrue(isValidPlaySeq(maxPlayersSeq, p6)),
			() -> assertTrue(allUniquePlaySeq(players))
		);
	}

	@Test
	@DisplayName("Should check all colors distributed by 3")
	void distributeColors3() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3);
		classicGameDist.distributeSeq(gamePlayers, players);

		assertAll(
			() -> assertTrue(allUniqueColor(players))
		);
	}

	@Test
	@DisplayName("Should check all colors distributed by 6")
	void distributeColors6() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3, p4, p5, p6);
		classicGameDist.distributeSeq(gamePlayers, players);

		assertAll(
			() -> assertTrue(allUniqueColor(players))
		);
	}

	private boolean allUniqueColor(List<ClassicGamePlayer> players) {
		Set<EGameColors> colors = new HashSet<>();
		for (ClassicGamePlayer player : players) {
			if (!colors.add(player.getColor())) {
				return false;
			}
		}
		return true;
	}

	private boolean allUniquePlaySeq(List<ClassicGamePlayer> players) {
		Set<Integer> seqs = new HashSet<>();
		for (ClassicGamePlayer player : players) {
			if (!seqs.add(player.getPlaySeq())) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidPlaySeq(int maxPlayersSeq, ClassicGamePlayer player) {
		return player.getPlaySeq() > 0 && player.getPlaySeq() <= maxPlayersSeq;
	}
}