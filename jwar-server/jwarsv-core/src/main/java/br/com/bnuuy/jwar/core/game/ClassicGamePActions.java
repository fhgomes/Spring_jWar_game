package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryCanBeTarget;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryHasAttackTroops;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isAddPhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isAttackPhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isCountryOwner;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isMyTurn;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.playerHasAvailableTroopsToAdd;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.utils.CardExchangeEvaluator;
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

	public void endCurrentTurn(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		//check if win (some conditions are valid on end of turn
		classicGame.turnToNextPlayer();
	}

	public void addTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
		playerHasAvailableTroopsToAdd(player, qtdTroops);

		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);

		//send update to other players country update, player update
	}

	public void addContTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		//validate continent troops available
//		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
//		validateOwnerAvailability(player, qtdTroops);

		//deduce from continent
//		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);
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

		// Validate country codes
		if (countryCodes == null || countryCodes.size() < 3) {
			throw new GameRulesException("Card exchange requires at least 3 cards");
		}

		// Get the cards to exchange
		List<EClassicCountryCard> playerCards = player.getCards();
		List<EClassicCountryCard> cardsToExchange = new java.util.ArrayList<>();

		for (Integer countryCode : countryCodes) {
			// Get the card for this country code
			EClassicCountryCard card = EClassicCountryCard.getByCountryCode(countryCode);

			// Check if the player has this card
			if (!playerCards.contains(card)) {
				throw new GameRulesException("Player does not have the card for country code: " + countryCode);
			}

			cardsToExchange.add(card);
		}

		// Validate the cards can be exchanged
		CardExchangeEvaluator.validateExchange(cardsToExchange);

		// Process the exchange
		classicGame.exchangeCards(player, cardsToExchange);

		// TODO: send update to other players about the card exchange
	}

}
