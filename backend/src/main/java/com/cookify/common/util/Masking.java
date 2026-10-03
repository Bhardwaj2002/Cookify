package com.cookify.common.util;

/** Partially hides personal data for display, e.g. "where we sent your code". */
public final class Masking {

    private Masking() {
    }

    /** {@code ayush@example.com} → {@code a***h@example.com}; short local parts keep only the first char. */
    public static String email(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String masked = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return masked + email.substring(at);
    }
}
