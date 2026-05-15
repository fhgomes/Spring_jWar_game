package br.com.bnuuy.jwar.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String message,
    String correlationId,
    Instant timestamp,
    List<FieldError> fieldErrors
) {

    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(code, message, correlationId, Instant.now(), null);
    }

    public static ErrorResponse of(String code, String message, String correlationId, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, correlationId, Instant.now(), fieldErrors);
    }
}
