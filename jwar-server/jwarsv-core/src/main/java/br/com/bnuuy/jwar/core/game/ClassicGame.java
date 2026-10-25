package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ADD;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ATTACK;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_MOVE;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.validatePlayersToStart;
import static java.lang.String.format;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import br.com.bnuuy.jwar.core.game.utils.CardExchangeState;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
import br.com.bnuuy.jwar.core.game.utils.EndGameEvaluator;
import br.com.bnuuy.jwar.core.game.utils.ExchangeCardsEvaluator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGame {

	public enum MatchStatus { LOBBY, IN_PROGRESS, FINISHED }

	private final ClassicGameDist classicGameDist;

	private final Map<Integer, ClassicGamePlayer> players;
	private final Map<Integer, ClassicGameCountry> countries;
	private final Map<Integer, ClassicGameContinent> continents;
	private final Map<Integer, List<ClassicGameContinent>> continentOwners;
	private final Map<EGameColors, ClassicGamePlayer> playersByColor;

	private ClassicGameAttackResProcessor attackResProcessor;

	// Objective cards and end game evaluation
	private EndGameEvaluator endGameEvaluator;

	// Card exchange evaluator
	private ExchangeCardsEvaluator exchangeCardsEvaluator;

	// Country cards deck
	private final List<EClassicCountryCard> cardsDeck;

	// Flag to track if the current player has conquered a country during their turn
	@Getter
	@Setter
	private boolean hasConqueredCountryThisTurn;

	@Getter
	private final CardExchangeState cardExchangeState;

	@Getter
	private final UUID matchId;

	@Getter
	private int qtdPlayers;

	@Getter
	@Setter
	private boolean firstRound;

	@Getter
	@Setter
	private boolean secondRound;

	@Getter
	@Setter
	private int currentPlayer;

	@Getter
	@Setter
	private int turnPhase;

	@Getter
	private MatchStatus matchStatus;

	@Getter
	private ClassicGamePlayer winner;

	/**
	 * Territories that have RECEIVED troops via the move action during the current
	 * player's move phase. Manual §8: each troop may move only once per turn —
	 * we enforce this by forbidding such territories from being a source of another
	 * move in the same phase.
	 */
	private final Set<Integer> movedTroopsInto;


	public ClassicGame(ClassicGameDist classicGameDist) {
		this.classicGameDist = classicGameDist;
		this.countries = new HashMap<>();
		this.players = new HashMap<>();
		this.continents = new HashMap<>();
		this.continentOwners = new HashMap<>();
		this.playersByColor = new HashMap<>();
		this.cardsDeck = new ArrayList<>();
		this.hasConqueredCountryThisTurn = false;
		this.cardExchangeState = new CardExchangeState();
		this.firstRound = true;
		this.secondRound = false;
		this.turnPhase = TURN_PHASE_ADD;
		this.matchId = UUID.randomUUID();
		this.matchStatus = MatchStatus.LOBBY;
		this.winner = null;
		this.movedTroopsInto = new HashSet<>();
	}

	public void startMatch(List<ClassicGamePlayer> lobbyPlayers) {
		validatePlayersToStart(lobbyPlayers);

		qtdPlayers = lobbyPlayers.size();

		// Reset card exchange count and prize
		cardExchangeState.reset();

		// Initialize and shuffle the country cards deck
		classicGameDist.initializeRoundCardsDeck(cardsDeck);

		// Initialize the end game evaluator
		endGameEvaluator = new EndGameEvaluator(continentOwners, playersByColor, players);

		// Initialize attack result processor
		attackResProcessor = new ClassicGameAttackResProcessor(continentOwners, endGameEvaluator, cardsDeck);

		// Initialize the card exchange evaluator
		exchangeCardsEvaluator = new ExchangeCardsEvaluator(countries, cardsDeck, cardExchangeState);

		// Initialize continents using the distributor
		classicGameDist.initializeContinents(continents);

		classicGameDist.distributeSeq(players, lobbyPlayers);
		classicGameDist.distributeColors(lobbyPlayers);
		// Map players by color for objective evaluation
		playersByColor.clear();
		for (ClassicGamePlayer player : lobbyPlayers) {
			playersByColor.put(player.getColor(), player);
		}

		// Initialize, shuffle, and distribute objective cards to players
		classicGameDist.distributeObjectiveCards(lobbyPlayers);

		// Distribute countries to players with continent references
		classicGameDist.distributeCountries(countries, continents, lobbyPlayers);

		// Initialize continent ownership and continentOwners map
		classicGameDist.initializeContinentOwners(continents, continentOwners);

		currentPlayer = 1;
		matchStatus = MatchStatus.IN_PROGRESS;
		turnToNextPlayer();
		//send update to all players
		//let all players know its first player turn
	}

	/**
	 * Advances the game to the next player's turn.
	 */
	public void turnToNextPlayer() {
		// Check if the current player has won at the end of their turn
		ClassicGamePlayer currentPlayerObj = players.get(currentPlayer);
		if (endGameEvaluator.hasPlayerWon(currentPlayerObj)) {
			log.info(format("Player [%s] has won the game by completing their objective at the end of their turn!",
				currentPlayerObj.getNickName()));
			finishMatch(currentPlayerObj);
			return;
		}

		// Check if the current player conquered a country during their turn
		// If so, give them a card (unless they already have the maximum)
		if (hasConqueredCountryThisTurn) {
			classicGameDist.drawCardForPlayer(cardsDeck, currentPlayerObj);

			// Reset the flag for the next player
			hasConqueredCountryThisTurn = false;
		}

		// New turn — reset per-turn move tracking (Manual §8)
		movedTroopsInto.clear();

		this.turnPhase = TURN_PHASE_ADD;
		setNextPlayer();
		if (firstRound || secondRound) {
			classicGameDist.distributeFirstRoundsTroops(qtdPlayers, players.get(currentPlayer));

			if (currentPlayer == qtdPlayers) {
				if (firstRound) {
					firstRound = false;
					secondRound = true;
				} else if (secondRound) {
					secondRound = false;
				}
			}
			return;
		}

		classicGameDist.distributeRoundTroops(players.get(currentPlayer), continentOwners.get(currentPlayer));
		//let all players know its next player turn
	}

	/**
	 * Transitions the match to FINISHED with the given winner.
	 * Called from turnToNextPlayer / attack when EndGameEvaluator detects victory.
	 */
	private void finishMatch(ClassicGamePlayer winner) {
		this.winner = winner;
		this.matchStatus = MatchStatus.FINISHED;
		log.info(format("Match [%s] finished. Winner: [%s].", matchId, winner.getNickName()));
	}

	private void setNextPlayer() {
		if (currentPlayer == qtdPlayers) {
			currentPlayer = 1;
			return;
		}
		currentPlayer++;
	}

	public ClassicGameCountry getCountry(int tgtCountry) {
		return countries.get(tgtCountry);
	}

	public ClassicGameContinent getContinent(int tgtCont) {
		return continents.get(tgtCont);
	}

	public ClassicGamePlayer getPlayer(int tgtPlayer) {
		return players.get(tgtPlayer);
	}

	public void endTurnAddPhase() {
		this.turnPhase = TURN_PHASE_ATTACK;
	}

	public void endTurnAttackPhase() {
		this.turnPhase = TURN_PHASE_MOVE;
	}

	public void endTurnMovePhase() {
		// Move phase is the last in a turn — advance to next player
		turnToNextPlayer();
	}

	/**
	 * Tracks that the given country received troops during the current move phase,
	 * preventing it from being a source for another move in the same turn
	 * (Manual §8 — "Um exército pode ser deslocado uma única vez").
	 */
	public void markMovedInto(int countryCode) {
		movedTroopsInto.add(countryCode);
	}

	public boolean hasMovedIntoThisTurn(int countryCode) {
		return movedTroopsInto.contains(countryCode);
	}

	/**
	 * Processes an attack between two countries.
	 *
	 * @param srcCountry the attacking country
	 * @param tgtCountry the defending country
	 * @return the result of the attack
	 */
	public AttackResultVO attack(ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		AttackResultVO attackRes = ClassicGameAttacker.attack(srcCountry, tgtCountry);

		// Use the instance of attackResProcessor to process the attack result
		attackResProcessor.process(attackRes, srcCountry, tgtCountry);

		// If the attack resulted in a conquest, set the flag
		if (attackRes.isConquered()) {
			hasConqueredCountryThisTurn = true;

			// Check if the attacking player has won
			ClassicGamePlayer attackingPlayer = srcCountry.getOwner();
			if (endGameEvaluator.hasPlayerWon(attackingPlayer)) {
				log.info(format("Player [%s] has won the game by completing their objective after conquering [%s]!",
					attackingPlayer.getNickName(), tgtCountry.getCountry().getName()));
				finishMatch(attackingPlayer);
			}
		}

		return attackRes;
	}

	/**
	 * Processes a card exchange for the specified player.
	 *
	 * @param player the player exchanging cards
	 * @param cardsToExchange the card codes to exchange
	 * @return the number of troops gained from the exchange
	 */
	public int exchangeCards(ClassicGamePlayer player, List<Integer> cardsToExchange) {
		// Process the card exchange using the evaluator
		return exchangeCardsEvaluator.processCardExchange(player, cardsToExchange);
	}


}
