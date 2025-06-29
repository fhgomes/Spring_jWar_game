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
			log.info(format("Player [%s] has been eliminated from the game!", defendingPlayer.getNickName()));
			result.setPlayerDestroyed(true);
			// Transfer cards from the defeated player to the attacking player
			transferCardsFromDefeatedPlayer(defendingPlayer, attackingPlayer);

			checkObjectiveAchievedEndGame(attackingPlayer, "Player [%s] has won the game by eliminating player [%s]!",
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

		log.info(format("Transferring [%d] cards from player [%s] to player [%s]",
			defeatedPlayerCards.size(), defeatedPlayer.getNickName(), attackingPlayer.getNickName()));

		// Calculate how many cards the attacking player can receive
		int availableSlots = ClassicGameConstants.MAX_CARDS - attackingPlayer.getCardCount();
		int cardsToTransfer = Math.min(availableSlots, defeatedPlayerCards.size());

		// Transfer cards up to the maximum
		for (int i = 0; i < cardsToTransfer; i++) {
			EClassicCountryCard card = defeatedPlayerCards.get(0);
			attackingPlayer.addCard(card);
			defeatedPlayerCards.remove(0);
			log.info(format("Transferred card for country [%s] to player [%s]",
				card.getCountry().getName(), attackingPlayer.getNickName()));
		}

		// Return any remaining cards to the deck
		if (!defeatedPlayerCards.isEmpty()) {
			log.info(format("Returning [%d] remaining cards to the deck", defeatedPlayerCards.size()));
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

			// Check if the player has won the game by conquering countries
			if (endGameEvaluator.hasPlayerWon(attackingPlayer)) {
				log.info(format("Player [%s] has won the game by conquering country [%s]!",
					attackingPlayer.getNickName(), tgtCountry.getCountry().getName()));
				return;
			}

			// Update continent ownership
			ClassicGameContinent continent = tgtCountry.getContinent();
			continent.checkAndUpdateOwnership();

			// Update continentOwners map
			updateContinentOwnership(continent);

			// Check if the player has won the game by conquering a continent
			checkObjectiveAchievedEndGame(attackingPlayer, "Player [%s] has won the game by conquering continent [%s]!",
				continent.getContinent().getName());
		}
	}

	private static void implyDmg(AttackResultVO result, ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		srcCountry.removeTroops(result.getSrcCountry());
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
