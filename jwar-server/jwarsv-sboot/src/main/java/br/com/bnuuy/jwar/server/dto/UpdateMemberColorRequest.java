package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberColorRequest(
    @NotBlank(message = "Cor é obrigatória")
    String color
) {}
