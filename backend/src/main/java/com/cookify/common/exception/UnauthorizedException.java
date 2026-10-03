package com.cookify.common.exception;

import org.springframework.http.HttpStatus;

/** Maps to HTTP 401 Unauthorized. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
