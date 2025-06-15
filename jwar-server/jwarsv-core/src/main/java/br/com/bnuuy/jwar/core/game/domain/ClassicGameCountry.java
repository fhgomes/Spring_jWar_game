package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import lombok.Getter;

@Getter
public class ClassicGameCountry {
	private final EClassicCountries country;
	private int troopsCount;
	private int gamePlayerOwner;
	private EGameColors playerColor;

	public ClassicGameCountry(EClassicCountries country, int gamePlayerOwner, EGameColors playerColor) {
		this.country = country;
		this.gamePlayerOwner = gamePlayerOwner;
		this.playerColor = playerColor;
		this.troopsCount = 1;
	}

	public void addTroops(int newTroops) {
		this.troopsCount += newTroops;
	}

	public void removeTroops(int outTroops) {
		this.troopsCount -= outTroops;
	}

	public void changeOwner(int gamePlayerOwner, EGameColors playerColor, int newTroops) {
		this.gamePlayerOwner = gamePlayerOwner;
		this.playerColor = playerColor;
		//todo validate here
		this.troopsCount = newTroops;
	}
}
