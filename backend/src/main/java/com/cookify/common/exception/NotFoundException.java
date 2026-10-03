package com.cookify.common.exception;

import org.springframework.http.HttpStatus;

/** Maps to HTTP 404 Not Found. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        this("NOT_FOUND", message);
    }

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
