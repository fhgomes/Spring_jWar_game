package br.com.bnuuy.jwar.core.game.utils;

import br.com.bnuuy.jwar.core.game.ClassicGameConstants;
import lombok.Getter;

/**
 * Encapsulates the state of card exchanges in the game.
 * Manages the count of exchanges and the current prize value.
 */
@Getter
public class CardExchangeState {
	// Track the number of card exchanges made in the game
	private int exchangeCount;
	// Track the current card exchange prize
    private int currentPrize;

    /**
     * Creates a new CardExchangeState with initial values.
     */
    public CardExchangeState() {
        reset();
    }

    /**
     * Resets the state to initial values.
     */
    public void reset() {
        exchangeCount = 0;
        currentPrize = ClassicGameConstants.INITIAL_EXCHANGE_TROOPS;
    }

    /**
     * Increments the exchange count and updates the prize value.
     *
     * @return the current prize value before the update
     */
    public int incrementExchangeCount() {
        int prize = currentPrize;
        exchangeCount++;

        // Update the prize for the next exchange
        if (currentPrize < ClassicGameConstants.EXCHANGE_INCREMENT_THRESHOLD) {
            currentPrize += ClassicGameConstants.EXCHANGE_INCREMENT_UNTIL_THRESHOLD;
        } else {
            currentPrize += ClassicGameConstants.EXCHANGE_INCREMENT_AFTER_THRESHOLD;
        }

        return prize;
    }
}