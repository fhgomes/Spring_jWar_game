package br.com.bnuuy.jwar.core.game.map;

import lombok.Getter;

@Getter
public enum EClassicCountries {
	// América do Sul
	BRA(1, "Brasil", EClassicContinents.AMS),
	ARG(2, "Argentina", EClassicContinents.AMS),
	PER(3, "Peru", EClassicContinents.AMS),
	VEN(4, "Venezuela", EClassicContinents.AMS),

	// América do Norte
	MEX(5, "México", EClassicContinents.AMN),
	CAL(6, "Califórnia", EClassicContinents.AMN),
	NVA(7, "Nova York", EClassicContinents.AMN),
	VAN(8, "Vancover", EClassicContinents.AMN),
	GRO(9, "Groenlândia", EClassicContinents.AMN),
	MAC(10, "Mackenzie", EClassicContinents.AMN),
	LAB(11, "Labrador", EClassicContinents.AMN),
	OTW(12, "Ottawa", EClassicContinents.AMN),
	ALA(12, "Alaska", EClassicContinents.AMN),

	// Europa
	ISL(13, "Islândia", EClassicContinents.EUR),
	ENG(14, "Inglaterra", EClassicContinents.EUR),
	ALE(15, "Alemanha", EClassicContinents.EUR),
	FRA(16, "França", EClassicContinents.EUR),
	SWD(17, "Sweden", EClassicContinents.EUR),
	POL(18, "Polônia", EClassicContinents.EUR),
	MOS(19, "Moscow", EClassicContinents.EUR),

	// África
	ARL(20, "Argélia", EClassicContinents.AFR),
	EGY(21, "Egito", EClassicContinents.AFR),
	SUD(22, "Sudão", EClassicContinents.AFR),
	CON(23, "Congo", EClassicContinents.AFR),
	MAD(24, "Madagascar", EClassicContinents.AFR),
	ADS(25, "África do Sul", EClassicContinents.AFR),

	// Oceania
	SUM(26, "Sumatra", EClassicContinents.OCE),
	AUS(27, "Austrália", EClassicContinents.OCE),
	NVG(28, "Nova Guiné", EClassicContinents.OCE),
	BOR(29, "Borneo", EClassicContinents.OCE),

	// Ásia
	SIB(30, "Sibéria", EClassicContinents.ASI),
	ORI(31, "Oriente Médio", EClassicContinents.ASI),
	ARA(32, "Aral", EClassicContinents.ASI),
	VIE(33, "Vietan", EClassicContinents.ASI),
	OMK(35, "Omsk", EClassicContinents.ASI),
	CHI(36, "China", EClassicContinents.ASI),
	MON(37, "Mongólia", EClassicContinents.ASI),
	JAP(38, "Japão", EClassicContinents.ASI),
	DUD(39, "Dudinka", EClassicContinents.ASI),
	CHT(40, "Chita", EClassicContinents.ASI),
	VLD(41, "Vladivostok", EClassicContinents.ASI),
	IND(42, "Índia", EClassicContinents.ASI);

	private final int code;
	private final String name;
	private final EClassicContinents continent;

	EClassicCountries(int code, String name, EClassicContinents continent) {
		this.code = code;
		this.name = name;
		this.continent = continent;
	}
}
