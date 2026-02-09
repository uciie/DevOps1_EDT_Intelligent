package com.example.backend.exception;

/**
 * Exception levée lorsque l'API Google Calendar est inaccessible ou renvoie une erreur.
 */
public class GoogleApiException extends RuntimeException {
    
    private final String errorCode;
    private final boolean isRetryable;

    public GoogleApiException(String message, String errorCode, boolean isRetryable) {
        super(message);
        this.errorCode = errorCode;
        this.isRetryable = isRetryable;
    }

    public GoogleApiException(String message, Throwable cause, String errorCode, boolean isRetryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.isRetryable = isRetryable;
    }

    public String getErrorCode() { return errorCode; }
    public boolean isRetryable() { return isRetryable; }
}