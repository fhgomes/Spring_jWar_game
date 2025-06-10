package br.com.bnuuy.jwar.core.game;

import br.com.bnuuy.jwar.core.game.map.EClassicContinents;
import lombok.Getter;

@Getter
public class ClassicGameContinent {

	private final EClassicContinents continent;
	private int availableTroopsCount;
	private int gamePlayerOwner;

	public ClassicGameContinent(EClassicContinents continent) {
		this.continent = continent;
		this.gamePlayerOwner = 0;
		this.availableTroopsCount = 0;
	}

	public void deduceTroops(int usedTroops) {
		this.availableTroopsCount -= usedTroops;
	}

	public void changeOwner(int gamePlayerOwner) {
		this.gamePlayerOwner = gamePlayerOwner;
	}

	public int getCode() {
		return this.continent.getCode();
	}

	public void addRoundTroops() {
		availableTroopsCount += continent.getReward();
	}
}
