package br.com.bnuuy.jwar.core.game.map;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ContinentCountriesUtil {

	private static final Map<EClassicContinents, List<EClassicCountries>> CONTINENTS = new EnumMap<>(EClassicContinents.class);

	static {
		CONTINENTS.put(EClassicContinents.AMS, List.of(
			EClassicCountries.BRA,
			EClassicCountries.ARG,
			EClassicCountries.PER,
			EClassicCountries.VEN
		));

		CONTINENTS.put(EClassicContinents.AMN, List.of(
			EClassicCountries.ALA,
			EClassicCountries.MEX,
			EClassicCountries.CAL,
			EClassicCountries.NVA,
			EClassicCountries.VAN,
			EClassicCountries.GRO,
			EClassicCountries.MAC,
			EClassicCountries.LAB,
			EClassicCountries.OTW
		));

		CONTINENTS.put(EClassicContinents.EUR, List.of(
			EClassicCountries.ISL,
			EClassicCountries.ENG,
			EClassicCountries.ALE,
			EClassicCountries.FRA,
			EClassicCountries.SWD,
			EClassicCountries.POL,
			EClassicCountries.MOS
		));

		CONTINENTS.put(EClassicContinents.AFR, List.of(
			EClassicCountries.ARL,
			EClassicCountries.EGY,
			EClassicCountries.SUD,
			EClassicCountries.CON,
			EClassicCountries.MAD,
			EClassicCountries.ADS
		));

		CONTINENTS.put(EClassicContinents.OCE, List.of(
			EClassicCountries.SUM,
			EClassicCountries.AUS,
			EClassicCountries.NVG,
			EClassicCountries.BOR
		));

		CONTINENTS.put(EClassicContinents.ASI, List.of(
			EClassicCountries.VLD,
			EClassicCountries.SIB,
			EClassicCountries.DUD,
			EClassicCountries.CHT,
			EClassicCountries.JAP,
			EClassicCountries.CHI,
			EClassicCountries.MON,
			EClassicCountries.VIE,
			EClassicCountries.IND,
			EClassicCountries.ARA,
			EClassicCountries.OMK,
			EClassicCountries.ORI
			));
	}

	public static List<EClassicCountries> getCountries(EClassicContinents continent) {
		return CONTINENTS.get(continent);
	}

	private ContinentCountriesUtil() {
		// utility class
	}
}
