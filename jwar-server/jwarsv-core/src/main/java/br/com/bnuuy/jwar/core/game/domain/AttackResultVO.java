package br.com.bnuuy.jwar.core.game.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@ToString
@AllArgsConstructor
public class AttackResultVO {
	private final int srcCountry;
	private final int targetCountry;

	private final int[] attackers;
	private final int[] defense;

	@Builder.Default
	@Setter
	private int srcCountryLoss = 0;

	@Builder.Default
	@Setter
	private int targetCountryLoss = 0;

	@Builder.Default
	@Setter
	private boolean conquered = false;

	@Builder.Default
	@Setter
	private boolean playerDestroyed = false;
}
