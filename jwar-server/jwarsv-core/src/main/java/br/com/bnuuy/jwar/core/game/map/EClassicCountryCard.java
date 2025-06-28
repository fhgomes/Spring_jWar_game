package br.com.bnuuy.jwar.core.game.map;

import static java.lang.String.format;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import lombok.Getter;

/**
 * Represents the country cards in the Classic War game.
 * Each card is associated with a country and has a shape (Triangle, Circle, or Square).
 */
@Getter
public enum EClassicCountryCard {
    // América do Sul
    BRA(EClassicCountries.BRA, ECardShape.TRIANGLE),
    ARG(EClassicCountries.ARG, ECardShape.CIRCLE),
    PER(EClassicCountries.PER, ECardShape.SQUARE),
    VEN(EClassicCountries.VEN, ECardShape.TRIANGLE),

    // América do Norte
    MEX(EClassicCountries.MEX, ECardShape.CIRCLE),
    CAL(EClassicCountries.CAL, ECardShape.SQUARE),
    NVA(EClassicCountries.NVA, ECardShape.TRIANGLE),
    VAN(EClassicCountries.VAN, ECardShape.CIRCLE),
    GRO(EClassicCountries.GRO, ECardShape.SQUARE),
    MAC(EClassicCountries.MAC, ECardShape.TRIANGLE),
    LAB(EClassicCountries.LAB, ECardShape.CIRCLE),
    OTW(EClassicCountries.OTW, ECardShape.SQUARE),
    ALA(EClassicCountries.ALA, ECardShape.TRIANGLE),

    // Europa
    ISL(EClassicCountries.ISL, ECardShape.CIRCLE),
    ENG(EClassicCountries.ENG, ECardShape.SQUARE),
    ALE(EClassicCountries.ALE, ECardShape.TRIANGLE),
    FRA(EClassicCountries.FRA, ECardShape.CIRCLE),
    SWD(EClassicCountries.SWD, ECardShape.SQUARE),
    POL(EClassicCountries.POL, ECardShape.TRIANGLE),
    MOS(EClassicCountries.MOS, ECardShape.CIRCLE),

    // África
    ARL(EClassicCountries.ARL, ECardShape.SQUARE),
    EGY(EClassicCountries.EGY, ECardShape.TRIANGLE),
    SUD(EClassicCountries.SUD, ECardShape.CIRCLE),
    CON(EClassicCountries.CON, ECardShape.SQUARE),
    MAD(EClassicCountries.MAD, ECardShape.TRIANGLE),
    ADS(EClassicCountries.ADS, ECardShape.CIRCLE),

    // Oceania
    SUM(EClassicCountries.SUM, ECardShape.SQUARE),
    AUS(EClassicCountries.AUS, ECardShape.TRIANGLE),
    NVG(EClassicCountries.NVG, ECardShape.CIRCLE),
    BOR(EClassicCountries.BOR, ECardShape.SQUARE),

    // Ásia
    SIB(EClassicCountries.SIB, ECardShape.TRIANGLE),
    ORI(EClassicCountries.ORI, ECardShape.CIRCLE),
    ARA(EClassicCountries.ARA, ECardShape.SQUARE),
    VIE(EClassicCountries.VIE, ECardShape.TRIANGLE),
    OMK(EClassicCountries.OMK, ECardShape.CIRCLE),
    CHI(EClassicCountries.CHI, ECardShape.SQUARE),
    MON(EClassicCountries.MON, ECardShape.TRIANGLE),
    JAP(EClassicCountries.JAP, ECardShape.CIRCLE),
    DUD(EClassicCountries.DUD, ECardShape.SQUARE),
    CHT(EClassicCountries.CHT, ECardShape.TRIANGLE),
    VLD(EClassicCountries.VLD, ECardShape.CIRCLE),
    IND(EClassicCountries.IND, ECardShape.SQUARE);

    private static final String NO_CARD_FOR_CODE = "No card found for country code: [%d]";
    private static final String NO_CARD_FOR_COUNTRY = "No card found for country: [%s]";

    private final EClassicCountries country;
    private final ECardShape shape;

    EClassicCountryCard(EClassicCountries country, ECardShape shape) {
        this.country = country;
        this.shape = shape;
    }

    /**
     * Gets the country card by country code.
     *
     * @param countryCode the country code
     * @return the country card
     * @throws GameRulesException if no card is found for the given country code
     */
    public static EClassicCountryCard getByCountryCode(int countryCode) {
        for (EClassicCountryCard card : values()) {
            if (card.getCountry().getCode() == countryCode) {
                return card;
            }
        }
        throw new GameRulesException(format(NO_CARD_FOR_CODE, countryCode));
    }

    /**
     * Gets the country card by country enum.
     *
     * @param country the country enum
     * @return the country card
     * @throws GameRulesException if no card is found for the given country
     */
    public static EClassicCountryCard getByCountry(EClassicCountries country) {
        for (EClassicCountryCard card : values()) {
            if (card.getCountry() == country) {
                return card;
            }
        }
        throw new GameRulesException(format(NO_CARD_FOR_COUNTRY, country.getName()));
    }
}
