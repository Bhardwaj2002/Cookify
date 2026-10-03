package com.cookify.common.exception;

import org.springframework.http.HttpStatus;

/** Maps to HTTP 403 Forbidden. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }

    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
