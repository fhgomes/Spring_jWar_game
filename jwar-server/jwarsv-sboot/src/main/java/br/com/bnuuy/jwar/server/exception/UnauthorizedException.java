package br.com.bnuuy.jwar.server.exception;

public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String message) {
        this(message, "UNAUTHENTICATED");
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
