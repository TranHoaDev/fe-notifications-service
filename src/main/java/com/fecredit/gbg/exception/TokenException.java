package com.fecredit.gbg.exception;

public class TokenException extends Exception {
    private final ErrorType errorType;

    public enum ErrorType {
        AUTHENTICATION_REQUIRED,
        TOKEN_EXCHANGE_FAILED,
        REFRESH_FAILED
    }

    public TokenException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public boolean requiresReAuthentication() {
        return errorType == ErrorType.AUTHENTICATION_REQUIRED;
    }
}