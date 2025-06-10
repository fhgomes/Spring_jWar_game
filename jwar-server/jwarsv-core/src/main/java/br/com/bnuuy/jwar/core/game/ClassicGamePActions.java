package br.com.bnuuy.jwar.core.game;

public class ClassicGamePActions {

	private final ClassicGame classicGame;
	private final ClassicGameValidator validator;

	public ClassicGamePActions(ClassicGame classicGame, ClassicGameValidator validator) {
		this.classicGame = classicGame;
		this.validator = validator;
	}

	public void attack(int srcPlayer, int srcCountry, int tgtCountry) {
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		validator.isAttackPhase(classicGame.getTurnPhase());
		validator.isCountryOwner(srcPlayer, classicGame.getCountry(srcCountry));
		validator.countryHasAttackTroops(classicGame.getCountry(srcCountry));
		validator.countryCanBeTarget(classicGame.getCountry(srcCountry), classicGame.getCountry(tgtCountry));

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
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAttackPhase();
	}

	public void endCurrentTurnAddPhase(int srcPlayer) {
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		classicGame.endTurnAddPhase();
	}

	public void endCurrentTurn(int srcPlayer) {
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		//check if win (some conditions are valid on end of turn
		classicGame.turnToNextPlayer();
	}

	public void addTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());

		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		validator.isCountryOwner(srcPlayer, country);

		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
		validator.playerHasAvailableTroopsToAdd(player, qtdTroops);

		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);

		//send update to other players country update, player update
	}

	public void addContTroops(int srcPlayer, int qtdTroops, int tgtCountry) {
		validator.isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
		ClassicGameCountry country = classicGame.getCountry(tgtCountry);
		validator.isCountryOwner(srcPlayer, country);

		//validate continent troops available
//		ClassicGamePlayer player = classicGame.getPlayer(srcPlayer);
//		validator.validateOwnerAvailability(player, qtdTroops);

		//deduce from continent
//		player.deduceTroops(qtdTroops);
		country.addTroops(qtdTroops);
	}

}
