package com.homekm.common;

public class ChildAccountWriteException extends RuntimeException {
    public ChildAccountWriteException() {
        super("Child accounts cannot perform this operation");
    }

    public ChildAccountWriteException(String message) {
        super(message);
    }
}
