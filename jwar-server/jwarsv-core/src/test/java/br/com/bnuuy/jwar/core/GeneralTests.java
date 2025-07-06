package br.com.bnuuy.jwar.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneralTests {

	@Test
	public void testCreatePlayer_WithOnlyNick(){

		var player1 = new BasePlayer("Toguro");
		System.out.println("Player name: "+ player1.getNick());

		assertEquals("Toguro", player1.getNick());

	}
}
