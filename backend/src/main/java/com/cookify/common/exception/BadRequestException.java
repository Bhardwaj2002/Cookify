package com.cookify.common.exception;

import org.springframework.http.HttpStatus;

/** Maps to HTTP 400 Bad Request. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        this("BAD_REQUEST", message);
    }

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
