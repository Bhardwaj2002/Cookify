package com.cookify.notification;

import com.cookify.config.AsyncConfig;
import com.cookify.notification.ActivityEvents.CommentAdded;
import com.cookify.notification.ActivityEvents.RecipePublished;
import com.cookify.notification.ActivityEvents.RecipeRated;
import com.cookify.notification.ActivityEvents.UserFollowed;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.social.comment.CommentRepository;
import com.cookify.social.follow.Follow;
import com.cookify.social.follow.FollowRepository;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Turns activity events into emails. Runs after the publishing transaction commits (so nothing
 * is sent for rolled-back actions) and on the notification thread pool (so requests don't wait).
 * Failures are logged and never affect the user's action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityNotifier {

    private final Notifier notifier;
    private final LinkBuilder links;
    private final RecipeRepository recipeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final Clock clock;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRecipePublished(RecipePublished event) {
        safely("new recipe " + event.recipeId(), () -> recipeRepository.findById(event.recipeId()).ifPresent(recipe -> {
            User author = recipe.getAuthor();
            List<Long> followerIds = followRepository.findByIdFolloweeId(author.getId()).stream()
                    .map(Follow::getId).map(Follow.Key::followerId).toList();
            Map<String, Object> data = Map.of(
                    "author", name(author),
                    "title", recipe.getTitle(),
                    "summary", recipe.getDescription() == null ? "" : recipe.getDescription(),
                    "link", links.recipe(recipe.getId()),
                    "profileLink", links.profile(author.getUsername()));
            int sent = 0;
            for (User follower : userRepository.findAllById(followerIds)) {
                if (!follower.isBannedAt(clock.instant())) {
                    notifier.send(follower, EmailTemplate.NEW_RECIPE, data);
                    sent++;
                }
            }
            log.info("Notified {} subscriber(s) of {} about recipe {}", sent, author.getUsername(), recipe.getId());
        }));
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onCommentAdded(CommentAdded event) {
        safely("comment " + event.commentId(), () -> commentRepository.findById(event.commentId()).ifPresent(comment -> {
            RecipeEntity recipe = comment.getRecipe();
            User owner = recipe.getAuthor();
            if (owner.getId().equals(comment.getAuthor().getId())) {
                return; // no email for commenting on your own recipe
            }
            notifier.send(owner, EmailTemplate.NEW_COMMENT, Map.of(
                    "commenter", name(comment.getAuthor()),
                    "title", recipe.getTitle(),
                    "comment", comment.getBody(),
                    "link", links.recipeComments(recipe.getId())));
        }));
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRecipeRated(RecipeRated event) {
        safely("rating of " + event.recipeId(), () -> recipeRepository.findById(event.recipeId()).ifPresent(recipe -> {
            User rater = userRepository.findById(event.raterId()).orElse(null);
            if (rater == null) {
                return;
            }
            notifier.send(recipe.getAuthor(), EmailTemplate.NEW_RATING, Map.of(
                    "rater", name(rater),
                    "title", recipe.getTitle(),
                    "stars", "★".repeat(event.score()) + "☆".repeat(5 - event.score()),
                    "average", recipe.getAvgRating().toPlainString(),
                    "count", recipe.getRatingCount(),
                    "link", links.recipe(recipe.getId())));
        }));
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onUserFollowed(UserFollowed event) {
        safely("follow " + event, () -> {
            User follower = userRepository.findById(event.followerId()).orElse(null);
            User followee = userRepository.findById(event.followeeId()).orElse(null);
            if (follower == null || followee == null) {
                return;
            }
            notifier.send(followee, EmailTemplate.NEW_FOLLOWER, Map.of(
                    "follower", name(follower),
                    "count", followRepository.countByIdFolloweeId(followee.getId()),
                    "link", links.profile(follower.getUsername())));
        });
    }

    private static String name(User u) {
        String full = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return full.isEmpty() ? u.getUsername() : full;
    }

    private static void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Failed to send notification for {}", what, e);
        }
    }
}
