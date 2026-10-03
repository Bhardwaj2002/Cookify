package com.cookify.common.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"abcdefghi", "Passw0rd!", "C00k!fy#2026", "a1!@#$%^&*()_+-=[]{}"})
    void acceptsValidPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "short123",          // 8 chars: below minimum of 9
            "has space123",      // contains a space
            "tab\there123",      // contains whitespace
            "pässwörd123",       // non-ASCII (restricted)
            "emoji🍕pass123"     // non-ASCII (restricted)
    })
    void rejectsInvalidPasswords(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }
}
