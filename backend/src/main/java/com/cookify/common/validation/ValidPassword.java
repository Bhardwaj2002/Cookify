package com.cookify.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password rule from the spec: at least 9 characters, no spaces, no restricted symbols.
 * Allowed: printable ASCII (letters, digits and !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~). Max 72 (BCrypt limit).
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be 9-72 characters long without spaces or restricted symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
