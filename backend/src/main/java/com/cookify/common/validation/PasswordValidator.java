package com.cookify.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    static final int MIN_LENGTH = 9;
    static final int MAX_LENGTH = 72;

    /** Printable ASCII excluding space (0x21–0x7E). */
    private static final Pattern ALLOWED = Pattern.compile("^[\\x21-\\x7E]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null
                && value.length() >= MIN_LENGTH
                && value.length() <= MAX_LENGTH
                && ALLOWED.matcher(value).matches();
    }
}
