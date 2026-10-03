package com.cookify.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all expected business errors. Services throw a subclass and
 * {@link GlobalExceptionHandler} turns it into a consistent JSON error response.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
