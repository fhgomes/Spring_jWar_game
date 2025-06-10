package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ADD;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_ATTACK;
import static br.com.bnuuy.jwar.core.game.ClassicGameConstants.TURN_PHASE_MOVE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import br.com.bnuuy.jwar.core.exceptions.GameRulesException;
import br.com.bnuuy.jwar.core.game.map.CountriesBordersUtil;
import java.util.HashSet;
import java.util.List;

public class ClassicGameValidator {

	public void validatePlayersToStart(List<ClassicGamePlayer> players) {
		if (isEmpty(players) || players.size() < 3) {
			throw new GameRulesException("Não é possível iniciar um jogo clássico com menos de 3 players");
		}

		if (players.size() > 6) {
			throw new GameRulesException("Não é possível iniciar um jogo clássico com mais de 6 players");
		}

		if (new HashSet<>(players).size() != players.size()) {
			throw new GameRulesException("Não é possível iniciar um jogo clássico jogadores repetidos");
		}
	}

	public void isCountryOwner(int srcPlayer, ClassicGameCountry classicGameCountry) {
		if (classicGameCountry.getGamePlayerOwner() != srcPlayer) {
			throw new GameRulesException("Não é possível exercutar a ação, o país de origem não pertence a voce");
		}
	}

	public void playerHasAvailableTroopsToAdd(ClassicGamePlayer player, int qtdTroops) {
		if (player.getAvailableTroops() < qtdTroops)
			throw new GameRulesException("Não é possível adicionar tropas, excede o limite disponível");
	}

	public void isMyTurn(int srcPlayer, int currentPlayer) {
		if (srcPlayer != currentPlayer) {
			throw new GameRulesException("Não é possível executar esta ação fora do seu turno");
		}
	}

	public void countryCanBeTarget(ClassicGameCountry src, ClassicGameCountry target) {
		if (src.getGamePlayerOwner() == target.getGamePlayerOwner()) {
			throw new GameRulesException("Não é possível atacar um país que te pertence");
		}
		if (!CountriesBordersUtil.hasBorder(src.getCountry(), target.getCountry())) {
			throw new GameRulesException("Não é possível atacar o país de ortigem e destino não tem fronteira");
		}
	}

	public void countryHasAttackTroops(ClassicGameCountry country) {
		if (country.getTroopsCount() < 2) {
			throw new GameRulesException("Não é possível atacar de um país com apenas 1 soldado");
		}
	}

	public void isMovePhase(int turnPhase) {
		if (turnPhase != TURN_PHASE_MOVE) {
			throw new GameRulesException("Não é possível mover tropas fora da fase de movimento");
		}
	}

	public void isAddPhase(int turnPhase) {
		if (turnPhase != TURN_PHASE_ADD) {
			throw new GameRulesException("Não é possível adicionar tropas fora da fase de adição");
		}
	}

	public void isAttackPhase(int turnPhase) {
		if (turnPhase != TURN_PHASE_ATTACK) {
			throw new GameRulesException("Não é possível atacar fora da fase de ataques");
		}
	}
}
