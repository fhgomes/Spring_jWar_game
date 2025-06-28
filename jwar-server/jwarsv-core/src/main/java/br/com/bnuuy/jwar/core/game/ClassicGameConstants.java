package br.com.bnuuy.jwar.core.game;

public class ClassicGameConstants {
	public static final int TURN_PHASE_ADD = 1;
	public static final int TURN_PHASE_ATTACK = 2;
	public static final int TURN_PHASE_MOVE = 3;

	// Maximum number of cards a player can hold
	public static final int MAX_CARDS = 5;

	// Card exchange constants
	public static final int INITIAL_EXCHANGE_TROOPS = 4;
	public static final int EXCHANGE_INCREMENT_UNTIL_THRESHOLD = 2;
	public static final int EXCHANGE_INCREMENT_AFTER_THRESHOLD = 5;
	public static final int EXCHANGE_INCREMENT_THRESHOLD = 10;
	public static final int COUNTRY_BONUS_TROOPS = 2;

	private ClassicGameConstants() {}

}
