package br.com.bnuuy.jwar.core.game;

import br.com.bnuuy.jwar.core.game.domain.AttackResultVO;
import br.com.bnuuy.jwar.core.game.domain.ClassicGameCountry;
import br.com.bnuuy.jwar.core.game.utils.ShufflerUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassicGameAttacker {

	public static AttackResultVO attack(ClassicGameCountry srcCountry, ClassicGameCountry tgtCountry) {
		int attackPos = getAttackPos(srcCountry.getTroopsCount());
		int defPos = getDefPos(tgtCountry.getTroopsCount());
		int[] attackers = new int[attackPos];
		int[] defense = new int[defPos];

		rollDices(attackers);
		rollDices(defense);

		return doResult(srcCountry, attackers, tgtCountry, defense);
	}

	private static AttackResultVO doResult(ClassicGameCountry srcCountry, int[] attackers,
										   ClassicGameCountry tgtCountry, int[] defense) {

		int srcLoss = 0;
		int tgtLoss = 0;
		int compared = Math.min(attackers.length, defense.length);
		for (int dpos = 0; dpos < compared; dpos++) {
			if (attackers[dpos] > defense[dpos]) {
				tgtLoss += 1;
			} else {
				srcLoss += 1;
			}
		}

		return AttackResultVO.builder()
			.srcCountry(srcCountry.getCountry().getCode())
			.targetCountry(tgtCountry.getCountry().getCode())
			.attackers(attackers)
			.defense(defense)
			.srcCountryLoss(srcLoss)
			.targetCountryLoss(tgtLoss)
			.build();
	}

	private static int getDefPos(int troopsCount) {
		if (troopsCount > 2) {
			return 3;
		}
		return Math.max(troopsCount, 0);
	}

	private static int getAttackPos(int troopsCount) {
		if (troopsCount < 1) {
			return 0;
		}
		if (troopsCount > 4) {
			return 3;
		}
		return troopsCount - 1;
	}

	private static void rollDices(int[] rollsPos) {
		for (int cd = 0; cd < rollsPos.length; cd++) {
			rollsPos[cd] = ShufflerUtil.rollDice();
		}

		ShufflerUtil.orderDices(rollsPos);
	}
}
