package br.com.bnuuy.jwar.core.game.map;

import lombok.Getter;

@Getter
public enum EGameColors {
	GRAY(1L, "Cinza"),
	YELLOW(2L, "Amarelo"),
	RED(3L, "Vermelho"),
	GREEN(4L, "Verde"),
	PURPLE(5L, "Roxo"),
	BLUE(6L, "Blue");

	private final long code;
	private final String name;

	EGameColors(long code, String name) {
		this.code = code;
		this.name = name;
	}
}
