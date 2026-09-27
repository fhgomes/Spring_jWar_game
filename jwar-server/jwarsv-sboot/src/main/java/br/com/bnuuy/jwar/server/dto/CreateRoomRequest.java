package br.com.bnuuy.jwar.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
    @NotBlank(message = "Nome da sala é obrigatório")
    @Size(max = 60, message = "Nome da sala deve ter no máximo 60 caracteres")
    String name,

    @Min(value = 3, message = "A sala deve ter pelo menos 3 jogadores")
    @Max(value = 6, message = "A sala deve ter no máximo 6 jogadores")
    int maxPlayers,

    String password
) {}
