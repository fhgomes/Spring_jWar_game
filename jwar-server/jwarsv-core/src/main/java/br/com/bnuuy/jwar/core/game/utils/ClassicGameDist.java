package br.com.bnuuy.jwar.core.game.utils;

import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shuffleColors;
import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shuffleCountries;
import static br.com.bnuuy.jwar.core.game.utils.ShufflerUtil.shufflePlayers;
import static java.lang.String.format;

import br.com.bnuuy.jwar.core.game.domain.ClassicGameContinent;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import br.com.bnuuy.jwar.core.game.map.EClassicCountries;
import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGameDist {

	private static final int MIN_ROUND_TROOPS = 3;

	private static final String PLAYER_POSITION =
		"Player: [%s] will play in position: [%s]";

	private static final String PLAYER_COLOR =
		"Player: [%s] will play as color: [%s]";

	private static final String PLAYER_COUNTRY=
		"Player: [%s] has earned the country: [%s]";


	public ClassicGameDist() {
	}

	public void distributeSeq(Map<Integer, ClassicGamePlayer> gamePlayers, List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> shufledPlayers = shufflePlayers(players);

		for (int i = 0; i < shufledPlayers.size(); i++) {
			int assignPlaySeq = i + 1;
			ClassicGamePlayer player = shufledPlayers.get(i);
			player.setPlaySeq(assignPlaySeq);
			gamePlayers.put(assignPlaySeq, player);

			log.info(format(PLAYER_POSITION, player.getNickName(), player.getPlaySeq()));
		}
	}

	public void distributeColors(List<ClassicGamePlayer> players) {
		List<EGameColors> shuffledColors = shuffleColors();

		for (int i = 0; i < players.size(); i++) {
			EGameColors assignColor = shuffledColors.get(i);
			ClassicGamePlayer player = players.get(i);
			player.setColor(assignColor);
			log.info(format(PLAYER_COLOR, player.getNickName(), player.getColor().getName()));
		}
	}

	public void distributeCountries(Map<Integer, ClassicGameCountry> gameCountries, List<ClassicGamePlayer> players) {
		List<ClassicGamePlayer> sortedPlayers = new ArrayList<>(players);
		sortedPlayers.sort(Comparator.comparingInt(ClassicGamePlayer::getPlaySeq));
		List<EClassicCountries> shuffledCountries = shuffleCountries();

		int distributed = 0;
		int ip = sortedPlayers.size()-1;

		for (int ic = 0; ic < shuffledCountries.size(); ic++) {
			EClassicCountries unassignedCountry = shuffledCountries.get(ic);
			ClassicGamePlayer player = sortedPlayers.get(ip);
			ClassicGameCountry gameCountry =
				new ClassicGameCountry(unassignedCountry, player.getPlaySeq(), player.getColor());
			player.earnCountry(gameCountry);
			gameCountries.put(gameCountry.getCountry().getCode(), gameCountry);
			distributed++;

			log.info(format(PLAYER_COUNTRY, player.getNickName(), gameCountry.getCountry().getName()));
			if (distributed == shuffledCountries.size()) {
				return;
			}
			if (ip == 0) {
				ip = sortedPlayers.size()-1;
			} else {
				ip--;
			}
		}
	}

	public void distributeFirstRoundsTroops(int qtdPlayers, ClassicGamePlayer player) {
		int troopsNewRound = getTroopsFirstRounds(qtdPlayers);
		player.addTroops(troopsNewRound);
	}


	public void distributeRoundTroops(ClassicGamePlayer player, List<ClassicGameContinent> classicGameContinents) {
		int troopsNewRound = MIN_ROUND_TROOPS;
		if (player.getOwnedCountries().size() > 7) {
			troopsNewRound = player.getOwnedCountries().size() /2;
		}
		player.addTroops(troopsNewRound);
		classicGameContinents.forEach(ClassicGameContinent::addRoundTroops);
	}

	private int getTroopsFirstRounds(int qtdPlayers) {
		int troopsNewRound = 0;
		if (qtdPlayers == 6) {
			troopsNewRound = 3;
		}
		if (qtdPlayers == 5) {
			troopsNewRound = 4;
		}
		if (qtdPlayers == 4) {
			troopsNewRound = 5;
		}
		if (qtdPlayers == 3) {
			troopsNewRound = 7;
		}
		return troopsNewRound;
	}


}
