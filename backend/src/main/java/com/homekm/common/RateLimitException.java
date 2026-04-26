package com.homekm.common;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("Too many login attempts, please try again later");
    }
}
