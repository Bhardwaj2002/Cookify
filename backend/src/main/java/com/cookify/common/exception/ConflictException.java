package com.cookify.common.exception;

import org.springframework.http.HttpStatus;

/** Maps to HTTP 409 Conflict. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        this("CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
