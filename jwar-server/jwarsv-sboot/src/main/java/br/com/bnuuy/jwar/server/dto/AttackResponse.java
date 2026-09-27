package br.com.bnuuy.jwar.server.dto;

public record AttackResponse(AttackResultDto attackResult, GameStateSnapshot snapshot) {}
