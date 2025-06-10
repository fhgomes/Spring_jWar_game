package br.com.bnuuy.jwar.core.game;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * In the future will allow to select colors during the lobby and carry on to match,
 * depending on the game and match type
 */
public class ClassicGameLobby {

	private final List<ClassicGamePlayer> players;
	private final ClassicGame classicGame;

	public ClassicGameLobby(ClassicGame classicGame) {
		this.classicGame = classicGame;
		this.players = new ArrayList<>();
	}

	public void joinLobby(ClassicGamePlayer newPlayer) {
		validateCanJoin(newPlayer);
		players.add(newPlayer);

		//when joining the lob, will register some id/connection to callback for updates
	}

	public void startMatch() {
		classicGame.startMatch(players);
	}

	private void validateCanJoin(ClassicGamePlayer newPlayer) {
		if (players.size() >= 6) {
			throw new GameRulesException("Não é póssível entrar, a sala está cheia");
		}

		if (new HashSet<>(players).contains(newPlayer)) {
			throw new GameRulesException("Este jogador já está no lobby");
		}
	}
}
