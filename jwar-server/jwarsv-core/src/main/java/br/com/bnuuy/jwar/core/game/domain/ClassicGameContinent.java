package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.map.EClassicContinents;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ClassicGameContinent {

	private final EClassicContinents continent;
	private int availableTroopsCount;
	private int gamePlayerOwner;
	private final List<ClassicGameCountry> countries;

	public ClassicGameContinent(EClassicContinents continent) {
		this.continent = continent;
		this.gamePlayerOwner = 0;
		this.availableTroopsCount = 0;
		this.countries = new ArrayList<>();
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

	public void addCountry(ClassicGameCountry country) {
		this.countries.add(country);
	}

	public boolean checkOwnership(int playerCode) {
		if (countries.isEmpty()) {
			return false;
		}

		return countries.stream()
			.allMatch(country -> country.getOwnerCode() == playerCode);
	}

	public void updateOwnership() {
		if (!countries.isEmpty() && countries.get(0).getOwner() != null) {
			int ownerCode = countries.get(0).getOwnerCode();
			if (checkOwnership(ownerCode)) {
				changeOwner(ownerCode);
			} else {
				changeOwner(0); // No single owner
			}
		}
	}
}
