package com.cookify.notification;

/**
 * Domain events published by the services and turned into emails by {@link ActivityNotifier}
 * after the transaction commits. They carry ids only; the listener reloads fresh data.
 */
public final class ActivityEvents {

    private ActivityEvents() {
    }

    /** A cook uploaded a recipe → email their subscribers. */
    public record RecipePublished(Long recipeId) {
    }

    /** Someone commented → email the recipe's author. */
    public record CommentAdded(Long commentId) {
    }

    /** Someone rated a recipe for the first time → email the recipe's author. */
    public record RecipeRated(Long recipeId, Long raterId, int score) {
    }

    /** Someone subscribed → email the cook. */
    public record UserFollowed(Long followerId, Long followeeId) {
    }
}
