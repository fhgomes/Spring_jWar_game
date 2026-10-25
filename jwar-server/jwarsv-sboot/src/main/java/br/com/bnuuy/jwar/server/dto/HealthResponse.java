package br.com.bnuuy.jwar.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Estado geral do serviço")
public record HealthResponse(
    @Schema(description = "Status do serviço", example = "UP") String status,
    @Schema(description = "Versão do build") String version,
    @Schema(description = "Commit Git incluído no build") String gitCommit,
    @Schema(description = "Momento da resposta") Instant timestamp
) {}
