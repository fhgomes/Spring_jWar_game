package br.com.bnuuy.jwar.core.game.utils;

import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shuffleColors;
import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shuffleCountries;
import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shufflePlayers;
import static java.lang.String.format;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicContinents;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGameDist {

	private static final int MIN_ROUND_TROOPS = 3;

	private static final String PLAYER_POSITION =
		"Player: [%s] will play in position: [%s]";

	private static final String PLAYER_COLOR =
		"Player: [%s] will play as color: [%s]";

	private static final String PLAYER_COUNTRY =
		"Player: [%s] has earned the country: [%s]";

	private static final String CARDS_DECK_INITIALIZED =
		"Country cards deck initialized and shuffled with [%d] cards";

	private static final String CANNOT_DRAW_EMPTY_DECK =
		"Cannot draw card: deck is empty";

	private static final String CANNOT_DRAW_MAX_CARDS =
		"Cannot draw card: player [%s] already has the maximum number of cards";

	private static final String PLAYER_DREW_CARD =
		"Player [%s] drew card for country [%s]";


	public ClassicGameDist() {
	}

	public void distributeSeq(Map<Integer, ClassicGamePlayer> gamePlayers, List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> shufledPlayers = shufflePlayers(players);

		for (int i = 0; i < shufledPlayers.size(); i++) {
			int assignPlaySeq = i + 1;
			ClassicGamePlayer player = shufledPlayers.get(i);
			player.setPlaySeq(assignPlaySeq);
			gamePlayers.put(assignPlaySeq, player);

			log.info(format(PLAYER_POSITION, player.getNickName(), player.getPlaySeq()));
		}
	}

	public void distributeColors(List<ClassicGamePlayer> players) {
		List<EGameColors> shuffledColors = shuffleColors();

		for (int i = 0; i < players.size(); i++) {
			EGameColors assignColor = shuffledColors.get(i);
			ClassicGamePlayer player = players.get(i);
			player.setColor(assignColor);
			log.info(format(PLAYER_COLOR, player.getNickName(), player.getColor().getName()));
		}
	}

	public void initializeContinents(Map<Integer, ClassicGameContinent> continents) {
		// Initialize continents
		for (EClassicContinents continentEnum : EClassicContinents.values()) {
			ClassicGameContinent continent = new ClassicGameContinent(continentEnum);
			continents.put(continentEnum.getCode(), continent);
		}
	}

	public void distributeCountries(Map<Integer, ClassicGameCountry> gameCountries,
									Map<Integer, ClassicGameContinent> continents,
									List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> sortedPlayers = new ArrayList<>(players);
		sortedPlayers.sort(Comparator.comparingInt(ClassicGamePlayer::getPlaySeq));
		List<EClassicCountries> shuffledCountries = shuffleCountries();

		int distributed = 0;
		int ip = sortedPlayers.size()-1;

		for (int ic = 0; ic < shuffledCountries.size(); ic++) {
			EClassicCountries unassignedCountry = shuffledCountries.get(ic);
			ClassicGamePlayer player = sortedPlayers.get(ip);

			// Get the continent for this country
			ClassicGameContinent continent = continents.get(unassignedCountry.getContinent().getCode());

			// Create the country with its continent
			ClassicGameCountry gameCountry =
				new ClassicGameCountry(unassignedCountry, continent, player, player.getColor());

			// Add the country to the player and to the game's countries map
			player.earnCountry(gameCountry);
			gameCountries.put(gameCountry.getCountry().getCode(), gameCountry);

			// Add the country to its continent
			continent.addCountry(gameCountry);

			distributed++;

			log.info(format(PLAYER_COUNTRY, player.getNickName(), gameCountry.getCountry().getName()));
			if (distributed == shuffledCountries.size()) {
				return;
			}
			if (ip == 0) {
				ip = sortedPlayers.size()-1;
			} else {
				ip--;
			}
		}
	}

	public void distributeFirstRoundsTroops(int qtdPlayers, ClassicGamePlayer player) {
		int troopsNewRound = getTroopsFirstRounds(qtdPlayers);
		player.addTroops(troopsNewRound);
	}


	public void distributeRoundTroops(ClassicGamePlayer player, List<ClassicGameContinent> classicGameContinents) {
		int troopsNewRound = MIN_ROUND_TROOPS;
		if (player.getOwnedCountries().size() > 7) {
			troopsNewRound = player.getOwnedCountries().size() /2;
		}
		player.addTroops(troopsNewRound);
		classicGameContinents.forEach(ClassicGameContinent::addRoundTroops);
	}

	/**
	 * Initializes the continentOwners map based on the current continent ownership
	 */
	public void initializeContinentOwners(Map<Integer, ClassicGameContinent> continents,
										 Map<Integer, List<ClassicGameContinent>> continentOwners) {
		// Clear the map first
		continentOwners.clear();

		// Update continent ownership and track in continentOwners map
		for (ClassicGameContinent continent : continents.values()) {
			continent.updateOwnership();

			// Update continentOwners map
			int ownerCode = continent.getGamePlayerOwner();
			if (ownerCode > 0) {
				if (!continentOwners.containsKey(ownerCode)) {
					continentOwners.put(ownerCode, new ArrayList<>());
				}
				continentOwners.get(ownerCode).add(continent);
			}
		}
	}

	private int getTroopsFirstRounds(int qtdPlayers) {
		int troopsNewRound = 0;
		if (qtdPlayers == 6) {
			troopsNewRound = 3;
		}
		if (qtdPlayers == 5) {
			troopsNewRound = 4;
		}
		if (qtdPlayers == 4) {
			troopsNewRound = 5;
		}
		if (qtdPlayers == 3) {
			troopsNewRound = 7;
		}
		return troopsNewRound;
	}

	/**
	 * Initializes and shuffles the country cards deck.
	 *
	 * @param cardsDeck the deck to initialize and shuffle
	 */
	public void initializeAndShuffleCardsDeck(List<EClassicCountryCard> cardsDeck) {
		// Clear the deck first
		cardsDeck.clear();

		// Add all country cards to the deck
		for (EClassicCountryCard card : EClassicCountryCard.values()) {
			cardsDeck.add(card);
		}

		// Shuffle the deck
		Collections.shuffle(cardsDeck);

		log.info(format(CARDS_DECK_INITIALIZED, cardsDeck.size()));
	}

	/**
	 * Draws a card from the deck and gives it to the player.
	 * If the deck is empty, it throws a GameRulesException.
	 * If the player already has the maximum number of cards, it throws a GameRulesException.
	 *
	 * @param cardsDeck the deck to draw from
	 * @param player the player to give the card to
	 * @return the card that was drawn
	 * @throws GameRulesException if the deck is empty or the player already has the maximum number of cards
	 */
	public EClassicCountryCard drawCardForPlayer(List<EClassicCountryCard> cardsDeck, ClassicGamePlayer player) {
		if (cardsDeck.isEmpty()) {
			log.info(format(CANNOT_DRAW_EMPTY_DECK));
			throw new GameRulesException(CANNOT_DRAW_EMPTY_DECK);
		}

		if (player.hasMaxCards()) {
			log.info(format(CANNOT_DRAW_MAX_CARDS, player.getNickName()));
			throw new GameRulesException(format(CANNOT_DRAW_MAX_CARDS, player.getNickName()));
		}

		EClassicCountryCard card = cardsDeck.remove(0);
		player.addCard(card);
		log.info(format(PLAYER_DREW_CARD, player.getNickName(), card.getCountry().getName()));
		return card;
	}
}
