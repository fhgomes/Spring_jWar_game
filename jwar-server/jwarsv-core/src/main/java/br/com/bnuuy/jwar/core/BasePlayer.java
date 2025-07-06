package br.com.bnuuy.jwar.core;

public class BasePlayer {

	//exemplo objeto/instancia player 1 -> nick = "Carlinhos"
	//exemplo objeto/instancia player 2 -> nick = "Tula Luana"
	private String nick;

	//exemplo objeto/instancia player 1 -> color = "vermelho"
	//exemplo objeto/instancia player 2 -> color = "verde"
	private String color;

	// Construtor somente com nick porque o color é atribuído automaticamente
	public BasePlayer(String nick) {
		this.nick = nick;
	}

	public BasePlayer(String pNick, String pColor) {
		this.nick = pNick;
		this.color = pColor;
	}

	public String getNick() {
		return nick;
	}

	public String getColor() {
		return color;
	}

	@Override
	public String toString() {
		return "BasePlayer{" +
				"nick='" + nick + '\'' +
				", color=" + color +
				'}';
	}

}
