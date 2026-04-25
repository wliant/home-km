package com.homekm.common;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, long id) {
        super(entity + " not found: " + id);
    }
}
