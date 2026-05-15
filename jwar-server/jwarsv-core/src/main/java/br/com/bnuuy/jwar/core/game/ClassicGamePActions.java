package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.continentHasAvailableTroopsToAdd;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryCanBeTarget;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryHasAttackTroops;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isAddPhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isAttackPhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isCountryOwner;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isMovePhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isMyTurn;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.playerHasAvailableTroopsToAdd;
import static java.lang.String.format;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.CountriesBordersUtil;
import br.com.bnuuy.jwar.core.game.utils.ExchangeCardsEvaluator;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGamePActions {

	private final ClassicGame classicGame;

	public ClassicGamePActions(ClassicGame classicGame) {
		this.classicGame = classicGame;
	}

	public void attack(int srcPlayer, int srcCountryId, int tgtCountryId) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		isAttackPhase(classicGame.getTurnPhase());

		ClassicGameCountry srcCountry = classicGame.getCountry(srcCountryId);
		isCountryOwner(srcPlayer, srcCountry);
		countryHasAttackTroops(srcCountry);

		ClassicGameCountry tgtCountry = classicGame.getCountry(tgtCountryId);
		countryCanBeTarget(srcCountry, tgtCountry);

		AttackResultVO attackRes = classicGame.attack(srcCountry, tgtCountry);

		log.info("Attack rolled: "+ Arrays.toString(attackRes.getAttackers()));
		log.info("Defense rolled: "+ Arrays.toString(attackRes.getDefense()));

	}

	public void endCurrentTurnAttackPhase(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAttackPhase();
	}

	public void endCurrentTurnAddPhase(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAddPhase();
	}

	public void endCurrentTurnMovePhase(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnMovePhase();
	}

	public void endCurrentTurn(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		//check if win (some conditions are valid on end of turn
		classicGame.turnToNextPlayer();
	}

	/**
	 * Moves troops between two contiguous own territories during the MOVE phase.
	 * Manual §8: source must keep at least 1 occupation troop; each troop may be
	 * moved only once per turn (territories that received troops cannot then be sources).
	 *
	 * @param srcPlayer the acting player
	 * @param srcCountryId source country code
	 * @param tgtCountryId target country code
	 * @param qtdTroops number of troops to move (>= 1)
	 * @throws GameRulesException if validation fails
	 */
	public void moveTroops(int srcPlayer, int srcCountryId, int tgtCountryId, int qtdTroops) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		isMovePhase(classicGame.getTurnPhase());

		if (qtdTroops < 1) {
			throw new GameRulesException("Quantidade de tropas a mover deve ser maior que zero");
		}
		if (srcCountryId == tgtCountryId) {
			throw new GameRulesException("Origem e destino do movimento devem ser diferentes");
		}

		ClassicGameCountry srcCountry = classicGame.getCountry(srcCountryId);
		ClassicGameCountry tgtCountry = classicGame.getCountry(tgtCountryId);
		isCountryOwner(srcPlayer, srcCountry);
		isCountryOwner(srcPlayer, tgtCountry);

		if (!CountriesBordersUtil.hasBorder(srcCountry.getCountry(), tgtCountry.getCountry())) {
			throw new GameRulesException("Não é possível mover tropas entre territórios não contíguos");
		}

		if (srcCountry.getTroopsCount() - qtdTroops < 1) {
			throw new GameRulesException("É preciso manter ao menos 1 exército de ocupação no território de origem");
		}

		if (classicGame.hasMovedIntoThisTurn(srcCountryId)) {
			throw new GameRulesException(
				"Um exército pode ser deslocado uma única vez no mesmo turno (Manual §8)");
		}

		srcCountry.removeTroops(qtdTroops);
		tgtCountry.addTroops(qtdTroops);
		classicGame.markMovedInto(tgtCountryId);

		log.info(format("Player [%d] moved [%d] troops from [%d] to [%d]",
			srcPlayer, qtdTroops, srcCountryId, tgtCountryId));
	}

	public void addTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
		playerHasAvailableTroopsToAdd(player, qtdTroops);

		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);

		//TODO SPRINT2 - COMNS - send update to other players country update, player update
	}

	public void addContinentTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		ClassicGameContinent continent = country.getContinent();
		continentHasAvailableTroopsToAdd(continent, qtdTroops);

		continent.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);

		//TODO SPRINT2 - COMNS - send update to other players country update, player update
	}

	/**
	 * Exchanges cards for troops during the add phase.
	 * The player must have at least 3 cards, and they must form a valid combination
	 * (3 of the same shape or 3 different shapes).
	 *
	 * @param srcPlayer the player exchanging cards
	 * @param countryCodes the country codes of the cards to exchange
	 * @throws GameRulesException if the exchange is invalid
	 */
	public void exchangeCards(int srcPlayer, List<Integer> countryCodes) {
		// Validate it's the player's turn and the add phase
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		isAddPhase(classicGame.getTurnPhase());

		// Get the player
		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);

		// Validate the cards can be exchanged
		ExchangeCardsEvaluator.validateExchange(player, countryCodes);

		// Process the exchange and get the troops gained
		int troopsGained = classicGame.exchangeCards(player, countryCodes);
		log.info(format("Player [%s] exchanged cards for [%d] troops", player.getNickName(), troopsGained));

		///TODO SPRINT2 - COMNS - send update to other players about the card exchange
	}

}
