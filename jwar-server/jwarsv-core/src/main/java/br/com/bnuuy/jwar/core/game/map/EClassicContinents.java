package br.com.bnuuy.jwar.core.game.map;

import lombok.Getter;

@Getter
public enum EClassicContinents {
	AMS(1, "America do Sul", 4, 2),
	AMN(2, "America do Norte", 9, 5),
	EUR(3, "Europa", 7, 5),
	AFR(4, "África", 6, 3),
	OCE(5, "Oceania", 4, 2),
	ASI(6, "America do Sul", 13, 7);

	private final int code;
	private final String name;
	private final int countries;
	private final int reward;

	EClassicContinents(int code, String name, int countries, int reward) {
		this.code = code;
		this.name = name;
		this.countries = countries;
		this.reward = reward;
	}
}
