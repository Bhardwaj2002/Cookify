package com.cookify.recipe.domain;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword lists for dietary checks. Matching is on whole words (so "eggplant" is not "egg") after
 * removing plant-based look-alikes (so "coconut milk" is not dairy).
 */
public final class IngredientRules {

    public static final Set<String> MEAT = Set.of(
            "chicken", "mutton", "lamb", "beef", "pork", "bacon", "ham", "sausage", "salami", "pepperoni",
            "turkey", "duck", "goat", "venison", "keema", "gelatin", "gelatine", "lard", "meat");

    public static final Set<String> SEAFOOD = Set.of(
            "fish", "salmon", "tuna", "prawn", "shrimp", "crab", "lobster", "squid", "octopus", "anchovy",
            "sardine", "mackerel", "pomfret", "hilsa", "cod", "clam", "mussel", "oyster", "scallop", "surimi");

    public static final Set<String> EGG = Set.of("egg", "mayonnaise", "mayo", "meringue");

    public static final Set<String> DAIRY_AND_HONEY = Set.of(
            "milk", "butter", "ghee", "cheese", "paneer", "curd", "yogurt", "yoghurt", "cream", "khoya",
            "mawa", "buttermilk", "whey", "honey", "malai", "dahi");

    /** Jain diet excludes root vegetables, bulbs and fungi (and honey). */
    public static final Set<String> JAIN_EXCLUDED = Set.of(
            "onion", "garlic", "potato", "carrot", "beetroot", "beet", "radish", "ginger", "shallot", "leek",
            "yam", "turnip", "mushroom", "honey");

    /** Plant-based or otherwise harmless phrases that contain a forbidden word. */
    private static final List<String> SAFE_PHRASES = List.of(
            "coconut milk", "almond milk", "soy milk", "oat milk", "rice milk", "cashew milk",
            "peanut butter", "almond butter", "cocoa butter", "coconut cream", "vegan cheese", "vegan butter",
            "cream of tartar", "dry ginger");

    private IngredientRules() {
    }

    /**
     * Returns the forbidden keyword that appears earliest in {@code ingredientName}, if any
     * (deterministic: "ginger garlic paste" always reports "ginger").
     */
    public static Optional<String> findForbidden(String ingredientName, Collection<String> forbidden) {
        String text = ingredientName.toLowerCase(Locale.ROOT);
        for (String safe : SAFE_PHRASES) {
            text = text.replace(safe, " ");
        }
        String earliest = null;
        int earliestIndex = Integer.MAX_VALUE;
        for (String word : forbidden) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(word) + "(s|es)?\\b").matcher(text);
            if (m.find() && m.start() < earliestIndex) {
                earliest = word;
                earliestIndex = m.start();
            }
        }
        return Optional.ofNullable(earliest);
    }
}
