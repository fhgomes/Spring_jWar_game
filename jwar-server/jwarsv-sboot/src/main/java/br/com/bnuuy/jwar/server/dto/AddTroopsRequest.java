package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddTroopsRequest(
    @NotNull(message = "Código do país é obrigatório")
    Integer countryCode,

    @Min(value = 1, message = "Quantidade de tropas deve ser ao menos 1")
    int qty
) {}
