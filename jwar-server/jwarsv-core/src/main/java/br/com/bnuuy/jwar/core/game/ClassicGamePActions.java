package br.com.bnuuy.jwar.core.game;

import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryCanBeTarget;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.countryHasAttackTroops;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isAttackPhase;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isCountryOwner;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.isMyTurn;
import static br.com.bnuuy.jwar.core.game.utils.ClassicGameValidator.playerHasAvailableTroopsToAdd;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.domain.ClassicGamePlayer;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGamePActions {

	private final ClassicGame classicGame;

	public ClassicGamePActions(ClassicGame classicGame) {
		this.classicGame = classicGame;
	}

	public void attack(int srcPlayer, int srcCountryId, int tgtCountryId) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		isAttackPhase(classicGame.getTurnPhase());

		ClassicGameCountry srcCountry = classicGame.getCountry(srcCountryId);
		isCountryOwner(srcPlayer, srcCountry);
		countryHasAttackTroops(srcCountry);

		ClassicGameCountry tgtCountry = classicGame.getCountry(tgtCountryId);
		countryCanBeTarget(srcCountry, tgtCountry);

		AttackResultVO attackRes = classicGame.attack(srcCountry, tgtCountry);

		log.info("Attack rolled: "+ Arrays.toString(attackRes.getAttackers()));
		log.info("Defense rolled: "+ Arrays.toString(attackRes.getDefense()));

		//verificar qtd dados ataque
		//verificar qtd dados defesa
		//rolar dados de ambos em paralelo, order por maiores
		//comparar atack x defesa
		//deduzir mortos ambos lados
		//verificar conquista
		//verificar conquista continente
		//verificar perda de continente
		//verificar player morreu
			//ganhou jogo?
			//passar cartas
	}

	public void endCurrentTurnAttackPhase(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAttackPhase();
	}

	public void endCurrentTurnAddPhase(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAddPhase();
	}

	public void endCurrentTurn(int srcPlayer) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		//check if win (some conditions are valid on end of turn
		classicGame.turnToNextPlayer();
	}

	public void addTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
		playerHasAvailableTroopsToAdd(player, qtdTroops);

		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);

		//send update to other players country update, player update
	}

	public void addContTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		isCountryOwner(srcPlayer, country);

		//validate continent troops available
//		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
//		validateOwnerAvailability(player, qtdTroops);

		//deduce from continent
//		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);
	}

}
