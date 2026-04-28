package com.homekm.common;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("Too many requests, please try again later");
    }

    public RateLimitException(String message) {
        super(message);
    }
}
