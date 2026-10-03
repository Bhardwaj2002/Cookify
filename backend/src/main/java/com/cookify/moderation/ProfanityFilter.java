package com.cookify.moderation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Detects abusive language from {@code moderation/blocked-words.txt}.
 * <ul>
 *   <li>whole words only, so "assess" or "Scunthorpe" are fine</li>
 *   <li>leetspeak is normalised first: "sh1t", "$hit", "b!tch"</li>
 *   <li>stretched letters still match: "shiiiit", "fuuuck"</li>
 * </ul>
 */
@Component
public class ProfanityFilter {

    private static final Map<Character, Character> LEET = Map.of(
            '0', 'o', '1', 'i', '!', 'i', '3', 'e', '4', 'a', '@', 'a', '5', 's', '$', 's', '7', 't');

    private record Rule(String word, Pattern pattern) {
    }

    private final List<Rule> rules;

    public ProfanityFilter() {
        this(load());
    }

    ProfanityFilter(List<String> words) {
        this.rules = words.stream().map(w -> new Rule(w, compile(w))).toList();
    }

    /** The first blocked word found in {@code text}, if any. */
    public Optional<String> findViolation(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(text);
        return rules.stream().filter(r -> r.pattern().matcher(normalized).find()).map(Rule::word).findFirst();
    }

    public boolean isClean(String text) {
        return findViolation(text).isEmpty();
    }

    /**
     * Lower-cases and undoes leetspeak. A symbol is only read as a letter when a letter (or another
     * leet symbol) follows it, so "sh1t" → "shit" but "shit!" keeps its exclamation mark.
     */
    private static String normalize(String text) {
        char[] chars = text.toLowerCase(Locale.ROOT).toCharArray();
        StringBuilder out = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            // "!" after a symbol is punctuation ("bsdk!!"), not a disguised letter.
            boolean beforeLetter = i + 1 < chars.length
                    && (Character.isLetter(chars[i + 1]) || (LEET.containsKey(chars[i + 1]) && chars[i + 1] != '!'));
            out.append(beforeLetter ? LEET.getOrDefault(c, c) : c);
        }
        return out.toString();
    }

    /** "ass" → \ba+s{2,}(?:s|es)?\b ; plurals match too ("idiots"). */
    private static Pattern compile(String phrase) {
        StringBuilder regex = new StringBuilder("\\b");
        String[] words = phrase.trim().split("\\s+");
        for (int w = 0; w < words.length; w++) {
            if (w > 0) {
                regex.append("\\s+");
            }
            String word = words[w];
            int i = 0;
            while (i < word.length()) {
                char c = word.charAt(i);
                int run = 1;
                while (i + run < word.length() && word.charAt(i + run) == c) {
                    run++;
                }
                regex.append(Pattern.quote(String.valueOf(c))).append(run == 1 ? "+" : "{" + run + ",}");
                i += run;
            }
        }
        return Pattern.compile(regex.append("(?:s|es)?\\b").toString());
    }

    private static List<String> load() {
        try {
            String content = new ClassPathResource("moderation/blocked-words.txt").getContentAsString(StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Missing moderation/blocked-words.txt", e);
        }
    }
}
