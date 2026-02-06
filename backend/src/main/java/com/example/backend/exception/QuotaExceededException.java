package com.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Cette annotation permet à Spring de renvoyer automatiquement une 429 si l'erreur remonte jusqu'au framework
@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}