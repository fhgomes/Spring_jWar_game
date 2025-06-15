package br.com.bnuuy.jwar.core.game.domain;

import br.com.bnuuy.jwar.core.game.map.EGameColors;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ClassicGamePlayer {

	private final String userId;
	private final String nickName;
	private EGameColors color;
	private int playSeq;
	private int availableTroops;
	private List<ClassicGameCountry> ownedCountries;


	public ClassicGamePlayer(String userId, String nickName) {
		this.userId = userId;
		this.nickName = nickName;
		this.availableTroops = 0;
		this.ownedCountries = new ArrayList<>();
	}

	public void setPlaySeq(int playSeq) {
		this.playSeq = playSeq;
	}

	public void setColor(EGameColors color) {
		this.color = color;
	}

	public List<ClassicGameCountry> getOwnedCountries() {
		return ownedCountries;
	}

	public void earnCountry(ClassicGameCountry unassignedCountry) {
		this.ownedCountries.add(unassignedCountry);
	}

	public void addTroops(int newTroops) {
		this.availableTroops += newTroops;
	}

	public void deduceTroops(int qtdTroops) {
		this.availableTroops -= qtdTroops;
	}
}
