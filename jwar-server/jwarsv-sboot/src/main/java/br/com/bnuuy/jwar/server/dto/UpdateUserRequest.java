package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 80, message = "Nome de exibição deve ter no máximo 80 caracteres")
    String displayName,

    @Size(max = 2048, message = "URL da foto deve ter no máximo 2048 caracteres")
    String photoUrl
) {}
