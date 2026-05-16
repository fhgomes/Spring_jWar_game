package br.com.bnuuy.jwar.core.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
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
class ClassicGameDistCountriesTest {

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
	Map<Integer, ClassicGameCountry> countries;
	Map<Integer, ClassicGameContinent> continents;

	@BeforeEach
	void setUp() {
		countries = new HashMap<>();
		continents = new HashMap<>();
		classicGameDist = new ClassicGameDist();
		p1.setPlaySeq(1);
		p2.setPlaySeq(2);
		p3.setPlaySeq(3);
		p4.setPlaySeq(4);
		p5.setPlaySeq(5);
		p6.setPlaySeq(6);

		// Initialize continents
		classicGameDist.initializeContinents(continents);
	}

	@Test
	@DisplayName("Should check all countries distributed by 3")
	void distributeCountries3() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3);
		classicGameDist.distributeCountries(countries, continents, players);

		assertAll(
			() -> assertTrue(allUniqueCountries(players)),
			() -> assertEquals(14, p1.getOwnedCountries().size()),
			() -> assertEquals(14, p2.getOwnedCountries().size()),
			() -> assertEquals(14, p3.getOwnedCountries().size())
		);
	}

	@Test
	@DisplayName("Should check all countries distributed by 4")
	void distributeCountries4() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3, p4);
		classicGameDist.distributeCountries(countries, continents, players);

		assertAll(
			() -> assertTrue(allUniqueCountries(players)),
			() -> assertEquals(10, p1.getOwnedCountries().size()),
			() -> assertEquals(10, p2.getOwnedCountries().size()),
			() -> assertEquals(11, p3.getOwnedCountries().size()),
			() -> assertEquals(11, p4.getOwnedCountries().size())
		);
	}

	@Test
	@DisplayName("Should check all countries distributed by 5")
	void distributeCountries5() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3, p4, p5);
		classicGameDist.distributeCountries(countries, continents, players);

		assertAll(
			() -> assertTrue(allUniqueCountries(players)),
			() -> assertEquals(8, p1.getOwnedCountries().size()),
			() -> assertEquals(8, p2.getOwnedCountries().size()),
			() -> assertEquals(8, p3.getOwnedCountries().size()),
			() -> assertEquals(9, p4.getOwnedCountries().size()),
			() -> assertEquals(9, p5.getOwnedCountries().size())
		);
	}

	@Test
	@DisplayName("Should check all countries distributed by 5 check orders")
	void distributeCountries5CheckInvertOrders() {
		List<ClassicGamePlayer> players = List.of(p2, p5, p1, p4, p3);
		classicGameDist.distributeCountries(countries, continents, players);

		assertAll(
			() -> assertTrue(allUniqueCountries(players)),
			() -> assertEquals(8, p1.getOwnedCountries().size()),
			() -> assertEquals(8, p2.getOwnedCountries().size()),
			() -> assertEquals(8, p3.getOwnedCountries().size()),
			() -> assertEquals(9, p4.getOwnedCountries().size()),
			() -> assertEquals(9, p5.getOwnedCountries().size())
		);
	}

	@Test
	@DisplayName("Should check all countries distributed by 6")
	void distributeCountries6() {
		List<ClassicGamePlayer> players = List.of(p1, p2, p3, p4, p5, p6);
		classicGameDist.distributeCountries(countries, continents, players);

		assertAll(
			() -> assertTrue(allUniqueCountries(players)),
			() -> assertEquals(7, p1.getOwnedCountries().size()),
			() -> assertEquals(7, p2.getOwnedCountries().size()),
			() -> assertEquals(7, p3.getOwnedCountries().size()),
			() -> assertEquals(7, p4.getOwnedCountries().size()),
			() -> assertEquals(7, p5.getOwnedCountries().size()),
			() -> assertEquals(7, p6.getOwnedCountries().size())
		);
	}


	private boolean allUniqueCountries(List<ClassicGamePlayer> players) {
		Set<ClassicGameCountry> countriesSet = new HashSet<>();
		for (ClassicGamePlayer player : players) {
			for (ClassicGameCountry country : player.getOwnedCountries()) {
				if (!countriesSet.add(country)) {
					return false;
				}
			}
		}
		return true;
	}
}
