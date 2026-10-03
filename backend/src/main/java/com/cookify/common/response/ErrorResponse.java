package com.cookify.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned by every failing request:
 * <pre>{ "error": { "code", "message", "fields"?, "path", "timestamp" } }</pre>
 */
public record ErrorResponse(Body error) {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Body(String code, String message, List<FieldError> fields, String path, Instant timestamp) {
    }

    /** A single invalid input field, e.g. {@code { "field": "email", "message": "must be a well-formed email address" }}. */
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message, String path) {
        return of(code, message, path, List.of());
    }

    public static ErrorResponse of(String code, String message, String path, List<FieldError> fields) {
        return new ErrorResponse(new Body(code, message, fields, path, Instant.now()));
    }
}
