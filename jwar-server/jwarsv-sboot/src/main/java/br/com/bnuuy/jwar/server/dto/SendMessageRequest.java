package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotBlank(message = "Texto da mensagem é obrigatório")
    @Size(max = 280, message = "Mensagem deve ter no máximo 280 caracteres")
    String text
) {}
