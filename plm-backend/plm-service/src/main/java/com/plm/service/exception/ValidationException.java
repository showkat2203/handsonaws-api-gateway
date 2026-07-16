package com.plm.service.exception;

/** Thrown when a request payload or state transition is invalid. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
