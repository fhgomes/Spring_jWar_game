package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ExchangeCardsRequest(
    @NotEmpty(message = "Lista de cartas é obrigatória")
    List<Integer> cardCodes
) {}
