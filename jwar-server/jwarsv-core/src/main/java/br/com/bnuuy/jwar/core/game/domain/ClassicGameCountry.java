package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import lombok.Getter;

@Getter
public class ClassicGameCountry {
	private final EClassicCountries country;
	private final ClassicGameContinent continent;
	private ClassicGamePlayer owner;
	private EGameColors playerColor;
	private int troopsCount;
	private int ownerCode;

	public ClassicGameCountry(EClassicCountries country, ClassicGameContinent continent,
							  ClassicGamePlayer owner, EGameColors playerColor) {
		this.country = country;
		this.continent = continent;
		this.owner = owner;
		this.ownerCode = owner.getPlaySeq();
		this.playerColor = playerColor;
		this.troopsCount = 1;
	}

	public void addTroops(int newTroops) {
		this.troopsCount += newTroops;
	}

	public void removeTroops(int outTroops) {
		this.troopsCount -= outTroops;
	}

	public void changeOwner(ClassicGamePlayer newOwner, EGameColors playerColor) {
		this.owner.getOwnedCountries().remove(this);
		this.owner = newOwner;
		this.ownerCode = newOwner.getPlaySeq();
		this.playerColor = playerColor;
	}
}
