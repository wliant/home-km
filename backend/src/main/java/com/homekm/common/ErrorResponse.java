package com.homekm.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final List<FieldError> errors;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
        this.errors = null;
    }

    public ErrorResponse(String code, List<FieldError> errors) {
        this.code = code;
        this.message = null;
        this.timestamp = Instant.now();
        this.errors = errors;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public List<FieldError> getErrors() { return errors; }
}
