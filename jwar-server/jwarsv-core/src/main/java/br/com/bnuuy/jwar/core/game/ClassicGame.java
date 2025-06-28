package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ADD;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ATTACK;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_MOVE;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.validatePlayersToStart;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.utils.ClassicGameDist;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGame {

	private final ClassicGameDist classicGameDist;

	private final Map<Integer, ClassicGamePlayer> players;
	private final Map<Integer, ClassicGameCountry> countries;
	private final Map<Integer, ClassicGameContinent> continents;
	private final Map<Integer, List<ClassicGameContinent>> continentOwners;

	private ClassicGameAttackResProcessor attackResProcessor;

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


	public ClassicGame(ClassicGameDist classicGameDist) {
		this.classicGameDist = classicGameDist;
		this.countries = new HashMap<>();
		this.players = new HashMap<>();
		this.continents = new HashMap<>();
		this.continentOwners = new HashMap<>();
		this.firstRound = true;
		this.secondRound = false;
		this.turnPhase = TURN_PHASE_ADD;
		this.matchId = UUID.randomUUID();
	}

	public void startMatch(List<ClassicGamePlayer> lobbyPlayers) {
		validatePlayersToStart(lobbyPlayers);
		classicGameDist.distributeSeq(players, lobbyPlayers);
		classicGameDist.distributeColors(lobbyPlayers);

		// Initialize continents using the distributor
		classicGameDist.initializeContinents(continents);

		// Distribute countries to players with continent references
		classicGameDist.distributeCountries(countries, continents, lobbyPlayers);

		// Initialize continent ownership and continentOwners map
		classicGameDist.initializeContinentOwners(continents, continentOwners);

		// Initialize attack result processor
		attackResProcessor = new ClassicGameAttackResProcessor(continentOwners);

		qtdPlayers = lobbyPlayers.size();
		currentPlayer = 1;

		turnToNextPlayer();
		//send update to all players
		//let all players know its first player turn
	}

	public void turnToNextPlayer() {
		this.turnPhase = TURN_PHASE_ADD;
		setNextPlayer();
		if (firstRound || secondRound) {
			classicGameDist.distributeFirstRoundsTroops(qtdPlayers, players.get(currentPlayer));

			if (currentPlayer == qtdPlayers) {
				if (firstRound) {
					firstRound = false;
					secondRound = true;
				}
				if (secondRound) {
					secondRound = false;
				}
			}
			return;
		}

		classicGameDist.distributeRoundTroops(players.get(currentPlayer), continentOwners.get(currentPlayer));
		//let all players know its next player turn

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

	public AttackResultVO attack(ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		AttackResultVO attackRes = ClassicGameAttacker.attack(srcCountry, tgtCountry);

		// Use the instance of attackResProcessor to process the attack result
		attackResProcessor.process(attackRes, srcCountry, tgtCountry);

		return attackRes;
	}
}
