package com.alzcare.eventingestion.service;

import org.springframework.http.HttpStatus;

/** A business error with the HTTP status it should produce. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
