package br.com.bnuuy.jwar.core.game;

import static java.lang.String.format;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.utils.EndGameEvaluator;
import br.com.bnuuy.jwar.core.game.utils.ExchangeCardsEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGameAttackResProcessor {

	private static final String PLAYER_HAS_BEEN_ELIMINATED = "Player [%s] has been eliminated from the game!";
	private static final String PLAYER_WON_THE_GAME_ELIMINATING_PLAYER =
		"Player [%s] has won the game by eliminating player [%s]!";
	private static final String TRANSFER_CARDS_FROM_ELIMINATED_PLAYER =
		"Transferring [%d] cards from player [%s] to player [%s]";
	private static final String TRANSFER_CARD_COUNTRY_TO_WON_PLAYER = "Transferred card for country [%s] to player [%s]";
	private static final String RETURN_REMAINING_CARDS_TO_DECK = "Returning [%d] remaining cards to the deck";
	private static final String PLAYER_WON_BY_CONQUERING_COUNTRIES =
		"Player [%s] has won the game by conquering country [%s]!";
	private static final String PLAYER_WON_BY_CONQUERING_CONTINENT =
		"Player [%s] has won the game by conquering continent [%s]!";

	private final Map<Integer, List<ClassicGameContinent>> continentOwners;
	private final EndGameEvaluator endGameEvaluator;
	private final List<EClassicCountryCard> cardsDeck;

	public ClassicGameAttackResProcessor(Map<Integer, List<ClassicGameContinent>> continentOwners,
										 EndGameEvaluator endGameEvaluator,
										 List<EClassicCountryCard> cardsDeck) {
		this.continentOwners = continentOwners;
		this.endGameEvaluator = endGameEvaluator;
		this.cardsDeck = cardsDeck;
	}

	public AttackResultVO process(AttackResultVO result, ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		implyDmg(result, srcCountry, tgtCountry);
		checkConquer(result, srcCountry, tgtCountry);

		// Check if the defending player has been eliminated
		ClassicGamePlayer defendingPlayer = tgtCountry.getOwner();
		ClassicGamePlayer attackingPlayer = srcCountry.getOwner();

		if (result.isConquered() && defendingPlayer.getOwnedCountries().isEmpty()) {
			log.info(format(PLAYER_HAS_BEEN_ELIMINATED, defendingPlayer.getNickName()));
			result.setPlayerDestroyed(true);
			// Transfer cards from the defeated player to the attacking player
			transferCardsFromDefeatedPlayer(defendingPlayer, attackingPlayer);

			checkObjectiveAchievedEndGame(attackingPlayer, PLAYER_WON_THE_GAME_ELIMINATING_PLAYER,
				defendingPlayer.getNickName());
		}

		return result;
	}

	private void checkObjectiveAchievedEndGame(ClassicGamePlayer attackingPlayer, String format,
											   String defendingPlayer) {
		// Check if the attacking player has won the game by eliminating another player
		if (endGameEvaluator.hasPlayerWon(attackingPlayer)) {
			log.info(format(format,
				attackingPlayer.getNickName(), defendingPlayer));
		}
	}

	/**
	 * Transfers cards from a defeated player to the attacking player.
	 * If the attacking player would exceed the maximum number of cards,
	 * only transfers enough cards to reach the maximum, and returns the rest to the deck.
	 *
	 * @param defeatedPlayer the player who was defeated
	 * @param attackingPlayer the player who defeated them
	 */
	private void transferCardsFromDefeatedPlayer(ClassicGamePlayer defeatedPlayer, ClassicGamePlayer attackingPlayer) {
		List<EClassicCountryCard> defeatedPlayerCards = defeatedPlayer.getCards();

		if (defeatedPlayerCards.isEmpty()) {
			return;
		}

		log.info(format(TRANSFER_CARDS_FROM_ELIMINATED_PLAYER,
			defeatedPlayerCards.size(), defeatedPlayer.getNickName(), attackingPlayer.getNickName()));

		// Calculate how many cards the attacking player can receive
		int availableSlots = ClassicGameConstants.MAX_CARDS - attackingPlayer.getCardCount();
		int cardsToTransfer = Math.min(availableSlots, defeatedPlayerCards.size());

		// Transfer cards up to the maximum
		for (int i = 0; i < cardsToTransfer; i++) {
			EClassicCountryCard card = defeatedPlayerCards.get(0);
			attackingPlayer.addCard(card);
			defeatedPlayerCards.remove(0);
			log.info(format(TRANSFER_CARD_COUNTRY_TO_WON_PLAYER,
				card.getCountry().getName(), attackingPlayer.getNickName()));
		}

		// Return any remaining cards to the deck
		if (!defeatedPlayerCards.isEmpty()) {
			log.info(format(RETURN_REMAINING_CARDS_TO_DECK, defeatedPlayerCards.size()));
			cardsDeck.addAll(defeatedPlayerCards);
			defeatedPlayerCards.clear();
		}

		// Update the attacking player's canExchangeCards flag
		boolean canExchange = ExchangeCardsEvaluator.canPlayerExchangeCards(attackingPlayer.getCards());
		attackingPlayer.updateCanExchangeCards(canExchange);
	}

	private void checkConquer(AttackResultVO result, ClassicGameCountry srcCountry,
							  ClassicGameCountry tgtCountry) {
		if (result.isConquered()) {
			ClassicGamePlayer attackingPlayer = srcCountry.getOwner();

			// Change ownership of the country
			tgtCountry.changeOwner(attackingPlayer, srcCountry.getPlayerColor());

			// Manual §7: transfer attacker dice count troops into the conquered country.
			// The maximum equals the number of dice used in the last attack; minimum is 1.
			// Default to the maximum so the conquered territory never sits at 0 troops.
			int troopsToMove = Math.max(1, result.getAttackers() == null ? 1 : result.getAttackers().length);
			troopsToMove = Math.min(troopsToMove, srcCountry.getTroopsCount() - 1);
			if (troopsToMove < 1) {
				troopsToMove = 1; // last-ditch: never leave conquered country at 0
			}
			srcCountry.removeTroops(troopsToMove);
			tgtCountry.addTroops(troopsToMove);

			// Check if the player has won the game by conquering countries
			if (endGameEvaluator.hasPlayerWon(attackingPlayer)) {
				log.info(format(PLAYER_WON_BY_CONQUERING_COUNTRIES,
					attackingPlayer.getNickName(), tgtCountry.getCountry().getName()));
				return;
			}

			// Update continent ownership
			ClassicGameContinent continent = tgtCountry.getContinent();
			continent.checkAndUpdateOwnership();

			// Update continentOwners map
			updateContinentOwnership(continent);

			// Check if the player has won the game by conquering a continent
			checkObjectiveAchievedEndGame(attackingPlayer, PLAYER_WON_BY_CONQUERING_CONTINENT,
				continent.getContinent().getName());
		}
	}

	private void implyDmg(AttackResultVO result, ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		srcCountry.removeTroops(result.getSrcCountryLoss());
		tgtCountry.removeTroops(result.getTargetCountryLoss());
		if (tgtCountry.getTroopsCount() < 1) {
			result.setConquered(true);
		}
	}

	/**
	 * Updates the continentOwners map when a continent's ownership changes
	 */
	private void updateContinentOwnership(ClassicGameContinent continent) {
		// First remove the continent from any player's list
		for (List<ClassicGameContinent> playerContinents : continentOwners.values()) {
			playerContinents.remove(continent);
		}

		// Then add it to the new owner's list if it has an owner
		int ownerCode = continent.getGamePlayerOwner();
		if (ownerCode > 0) {
			if (!continentOwners.containsKey(ownerCode)) {
				continentOwners.put(ownerCode, new ArrayList<>());
			}
			continentOwners.get(ownerCode).add(continent);
		}
	}
}
