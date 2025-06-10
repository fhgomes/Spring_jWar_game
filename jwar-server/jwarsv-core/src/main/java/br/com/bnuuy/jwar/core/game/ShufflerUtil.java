package br.com.bnuuy.jwar.core.game;

import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShufflerUtil {

	public List<EGameColors> shuffleColors() {
		List<EGameColors> colors = new ArrayList<>(List.of(EGameColors.values()));
		Collections.shuffle(colors);
		return colors;
	}

	public List<ClassicGamePlayer> shufflePlayers(List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> shuffled = new ArrayList<>(players);
		Collections.shuffle(shuffled);
		return shuffled;
	}

	public List<EClassicCountries> shuffleCountries() {
		List<EClassicCountries> shuffled = new ArrayList<>(List.of(EClassicCountries.values()));
		Collections.shuffle(shuffled);
		return shuffled;
	}

}
