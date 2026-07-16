package com.plm.service.exception;

/** Thrown when a requested entity does not exist. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException forEntity(String entityType, String id) {
        return new NotFoundException("%s not found: %s".formatted(entityType, id));
    }
}
