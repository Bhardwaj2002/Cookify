package com.cookify.notification;

/** Every email the platform sends: subject line + HTML body file under {@code templates/email/}. */
public enum EmailTemplate {

    LOGIN_OTP("Your Cookify login code", "login-otp"),
    PASSWORD_RESET("Reset your Cookify password", "password-reset"),
    ACCOUNT_WARNING("A warning on your Cookify account", "account-warning"),
    ACCOUNT_BANNED("Your Cookify account has been suspended", "account-banned"),
    NEW_RECIPE("New recipe from a cook you follow", "new-recipe"),
    NEW_COMMENT("New comment on your recipe", "new-comment"),
    NEW_RATING("Your recipe was rated", "new-rating"),
    NEW_FOLLOWER("You have a new subscriber", "new-follower");

    private final String subject;
    private final String fileName;

    EmailTemplate(String subject, String fileName) {
        this.subject = subject;
        this.fileName = fileName;
    }

    public String subject() {
        return subject;
    }

    public String fileName() {
        return fileName;
    }
}
