package com.cookify.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingTest {

    @ParameterizedTest
    @CsvSource({
            "ayush@example.com, a***h@example.com",
            "ab@example.com,    a***@example.com",
            "a@example.com,     a***@example.com",
            "not-an-email,      ***"
    })
    void masksEmailLocalPart(String input, String expected) {
        assertThat(Masking.email(input)).isEqualTo(expected);
    }
}
