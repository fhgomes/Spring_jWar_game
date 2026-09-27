package br.com.bnuuy.jwar.core.game.map;

import static br.com.bnuuy.jwar.core.game.map.EClassicCountries.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CountriesBordersUtil {

	private static final Map<EClassicCountries, List<EClassicCountries>> BORDERS = new EnumMap<>(EClassicCountries.class);

	static {

		// América do Sul
		BORDERS.put(BRA, List.of(ARG, PER, VEN));
		BORDERS.put(ARG, List.of(BRA, PER));
		BORDERS.put(PER, List.of(ARG, BRA, VEN));
		BORDERS.put(VEN, List.of(PER, BRA, MEX));

		// América do Norte
		BORDERS.put(MEX, List.of(VEN, CAL, NVA));
		BORDERS.put(CAL, List.of(MEX, NVA, OTW, VAN));
		BORDERS.put(NVA, List.of(MEX, OTW, CAL, LAB));
		BORDERS.put(VAN, List.of(CAL, OTW, MAC, GRO, ALA));
		BORDERS.put(OTW, List.of(CAL, NVA, LAB, VAN, MAC));
		BORDERS.put(LAB, List.of(VAN, GRO, OTW, NVA));
		BORDERS.put(GRO, List.of(MAC, LAB, ISL));
		BORDERS.put(MAC, List.of(ALA, VAN, GRO, OTW));

		// Europa
		BORDERS.put(ISL, List.of(GRO, ENG));
		BORDERS.put(ENG, List.of(ISL, ALE, FRA, SWD));
		BORDERS.put(ALE, List.of(ENG, FRA, POL));
		BORDERS.put(FRA, List.of(ENG, ALE, POL, ARL, EGY));
		BORDERS.put(SWD, List.of(ENG, MOS));
		BORDERS.put(POL, List.of(FRA, ALE, MOS, EGY));
		BORDERS.put(MOS, List.of(SWD, POL, OMK, ARA, ORI));

		// África
		BORDERS.put(ARL, List.of(BRA, FRA, EGY, SUD, CON));
		BORDERS.put(EGY, List.of(ARL, ORI, SUD, FRA, POL));
		BORDERS.put(SUD, List.of(ARL, EGY, CON, MAD, ADS));
		BORDERS.put(CON, List.of(SUD, ARL, ADS));
		BORDERS.put(ADS, List.of(CON, MAD, SUD));
		BORDERS.put(MAD, List.of(SUD, ADS));

		// Oceania
		BORDERS.put(AUS, List.of(SUM, BOR, NVG));
		BORDERS.put(NVG, List.of(IND, NVG, AUS));
		BORDERS.put(BOR, List.of(NVG, AUS, VIE));
		BORDERS.put(SUM, List.of(AUS, IND));

		// Ásia
		BORDERS.put(VLD, List.of(ALA, SIB, CHI, CHT, JAP));
		BORDERS.put(SIB, List.of(VLD, CHT, DUD));
		BORDERS.put(JAP, List.of(VLD, CHI));
		BORDERS.put(CHT, List.of(SIB, VLD, DUD, MON));
		BORDERS.put(CHI, List.of(VLD, JAP, VIE, IND, ARA, MON, CHT, OMK));
		BORDERS.put(VIE, List.of(BOR, IND, CHI));
		BORDERS.put(IND, List.of(CHI, SUM, VIE, ARA, ORI));
		BORDERS.put(ORI, List.of(EGY, POL, MOS, ARA, IND));
		BORDERS.put(ARA, List.of(MOS, ORI, IND, CHI, OMK));
		BORDERS.put(OMK, List.of(MOS, ARA, CHI, MON, DUD));
		BORDERS.put(MON, List.of(CHI, OMK, DUD, CHT));
		BORDERS.put(DUD, List.of(OMK, MON, CHT, SIB));
	}

	public static boolean hasBorder(EClassicCountries src, EClassicCountries tgt) {
		return BORDERS.get(src).contains(tgt);
	}

	private CountriesBordersUtil() {
		// utility class
	}
}
