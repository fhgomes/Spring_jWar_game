package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import br.com.bnuuy.jwar.core.game.map.EObjectiveCard;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ClassicGamePlayer {

	private final String userId;
	private final String nickName;
	private EGameColors color;
	private int playSeq;
	private int availableTroops;
	private final List<ClassicGameCountry> ownedCountries;
	private final List<EClassicCountryCard> cards;
	private EObjectiveCard gameObjective;

	@Setter
	private boolean canExchangeCards;


	public ClassicGamePlayer(String userId, String nickName) {
		this.userId = userId;
		this.nickName = nickName;
		this.availableTroops = 0;
		this.ownedCountries = new ArrayList<>();
		this.cards = new ArrayList<>();
		this.canExchangeCards = false;
	}

	public void assignObjective(EObjectiveCard gameObjective) {
		this.gameObjective = gameObjective;
	}

	public void setPlaySeq(int playSeq) {
		this.playSeq = playSeq;
	}

	public void setColor(EGameColors color) {
		this.color = color;
	}

	public List<ClassicGameCountry> getOwnedCountries() {
		return ownedCountries;
	}

	public void earnCountry(ClassicGameCountry unassignedCountry) {
		unassignedCountry.changeOwner(this, this.getColor());
		this.ownedCountries.add(unassignedCountry);
	}

	public void addTroops(int newTroops) {
		this.availableTroops += newTroops;
	}

	public void deduceTroops(int qtdTroops) {
		this.availableTroops -= qtdTroops;
	}

	/**
	 * Adds a country card to the player's hand if they haven't reached the maximum number of cards.
	 *
	 * @param card the country card to add
	 * @return true if the card was added, false if the player already has the maximum number of cards
	 */
	public boolean addCard(EClassicCountryCard card) {
		if (cards.size() >= ClassicGameConstants.MAX_CARDS) {
			return false;
		}
		cards.add(card);
		return true;
	}

	/**
	 * Checks if the player has reached the maximum number of cards.
	 *
	 * @return true if the player has the maximum number of cards, false otherwise
	 */
	public boolean hasMaxCards() {
		return cards.size() >= ClassicGameConstants.MAX_CARDS;
	}

	/**
	 * Gets the number of cards in the player's hand.
	 *
	 * @return the number of cards
	 */
	public int getCardCount() {
		return cards.size();
	}

	/**
	 * Updates the canExchangeCards flag with the given value.
	 *
	 * @param canExchange the new value for canExchangeCards
	 * @return the new value of canExchangeCards
	 */
	public void updateCanExchangeCards(boolean canExchange) {
		this.canExchangeCards = canExchange;
	}

	/**
	 * Removes the specified cards from the player's hand.
	 *
	 * @param cardsToRemove the cards to remove
	 * @return true if all cards were removed successfully, false otherwise
	 */
	public void removeExchangedCards(List<EClassicCountryCard> cardsToRemove) {
		if (cardsToRemove == null || cardsToRemove.isEmpty()) {
			return;
		}

		cards.removeAll(cardsToRemove);

		// Update the canExchangeCards flag after removing cards
		canExchangeCards = false;
	}
}
