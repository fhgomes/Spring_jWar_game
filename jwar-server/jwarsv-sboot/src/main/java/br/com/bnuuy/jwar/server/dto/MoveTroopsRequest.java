package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTroopsRequest(
    @NotNull(message = "País de origem é obrigatório")
    Integer srcCountryCode,

    @NotNull(message = "País de destino é obrigatório")
    Integer tgtCountryCode,

    @Min(value = 1, message = "Quantidade de tropas deve ser ao menos 1")
    int qty
) {}
