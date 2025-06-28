package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import br.com.bnuuy.jwar.core.game.map.EClassicCountryCard;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ClassicGamePlayer {

	private final String userId;
	private final String nickName;
	private EGameColors color;
	private int playSeq;
	private int availableTroops;
	private final List<ClassicGameCountry> ownedCountries;
	private final List<EClassicCountryCard> cards;


	public ClassicGamePlayer(String userId, String nickName) {
		this.userId = userId;
		this.nickName = nickName;
		this.availableTroops = 0;
		this.ownedCountries = new ArrayList<>();
		this.cards = new ArrayList<>();
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
}
