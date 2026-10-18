package br.com.bnuuy.jwar.core.game.utils;

import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

public class ShufflerUtil {

	private ShufflerUtil() {}

	public static List<EGameColors> shuffleColors() {
		List<EGameColors> colors = new ArrayList<>(List.of(EGameColors.values()));
		Collections.shuffle(colors);
		return colors;
	}

	public static List<ClassicGamePlayer> shufflePlayers(List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> shuffled = new ArrayList<>(players);
		Collections.shuffle(shuffled);
		return shuffled;
	}

	public static List<EClassicCountries> shuffleCountries() {
		List<EClassicCountries> shuffled = new ArrayList<>(List.of(EClassicCountries.values()));
		Collections.shuffle(shuffled);
		return shuffled;
	}

	public static int rollDice() {
		RandomGenerator random = RandomGenerator.getDefault();
		return random.nextInt(1, 7);
	}

	public static void orderDices(int[] rollsPos) {
		Arrays.sort(rollsPos); // Sort ascending
		// Reverse to descending
		for (int i = 0; i < rollsPos.length / 2; i++) {
			int temp = rollsPos[i];
			rollsPos[i] = rollsPos[rollsPos.length - 1 - i];
			rollsPos[rollsPos.length - 1 - i] = temp;
		}
	}
}
