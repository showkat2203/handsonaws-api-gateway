package com.plm.service.exception;

/** Thrown when the calling principal lacks the role required for an operation. */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}
