package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email(message = "Formato de e-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    String email,

    @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
    @NotBlank(message = "Senha é obrigatória")
    String password,

    @NotBlank(message = "Nome de exibição é obrigatório")
    @Size(max = 80, message = "Nome de exibição deve ter no máximo 80 caracteres")
    String displayName
) {}
