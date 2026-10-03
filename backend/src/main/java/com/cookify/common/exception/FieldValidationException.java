package com.cookify.common.exception;

import com.cookify.common.response.ErrorResponse.FieldError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/** Business-rule validation failure reported per field, in the same shape as bean-validation errors. */
@Getter
public class FieldValidationException extends ApiException {

    private final List<FieldError> fields;

    public FieldValidationException(List<FieldError> fields) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid");
        this.fields = List.copyOf(fields);
    }
}
