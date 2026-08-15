package com.banksecurity.backend.exception;

import lombok.Getter;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception personnalisée pour les erreurs de validation
 * Correspond au statut HTTP 422 (Unprocessable Entity)
 */
@Getter
@ResponseStatus(value = org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
@SuppressWarnings({"unused", "deprecation"})
public class ValidationException extends RuntimeException {

    private final String field;

    /**
     * Constructeur avec message uniquement
     */
    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    /**
     * Constructeur avec champ et message
     */
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Constructeur avec message et cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }
}