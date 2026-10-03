package com.cookify.moderation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "this is shit",
            "SHIT recipe",
            "sh1t",           // leetspeak
            "$hit",
            "shiiiiit",       // stretched
            "you idiot",
            "what a moron.",
            "total bullshit!",
            "chutiya",
            "bsdk",           // Hinglish short form
            "BSDK!!",
            "garbage   cook", // phrase with extra spaces
    })
    void blocksAbuse(String text) {
        assertThat(filter.isClean(text)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Loved this dal, thank you!",
            "I assessed the spices carefully",   // "ass" inside a word
            "Classic Scunthorpe pudding",
            "Shiitake mushrooms work well",
            "stupidly good recipe",               // only whole words are blocked
            "Add a dash of hing",
            "",
    })
    void allowsNormalText(String text) {
        assertThat(filter.isClean(text)).isTrue();
    }

    @Test
    void reportsTheMatchedWord() {
        assertThat(filter.findViolation("such a l0ser")).contains("loser");
    }
}
