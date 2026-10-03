package com.cookify.moderation;

import com.cookify.media.StorageService;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.entity.RecipeMediaEntity;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.social.comment.CommentRepository;
import com.cookify.social.follow.FollowRepository;
import com.cookify.social.rating.RatingRepository;
import com.cookify.social.saved.SavedRecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Removes everything a banned user contributed (spec: "removing all comments, recipes uploaded,
 * ratings and interaction history"), keeping other recipes' rating averages and comment counts correct.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentPurger {

    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final FollowRepository followRepository;
    private final StorageService storage;

    public record Summary(int ratings, int comments, int recipes, int follows) {
    }

    @Transactional
    public Summary purge(Long userId) {
        // Ratings: reverse each score on the rated recipe before deleting.
        List<Object[]> scores = ratingRepository.findScoresByUser(userId);
        for (Object[] row : scores) {
            recipeRepository.applyRating((Long) row[0], -((Number) row[1]).intValue(), -1);
        }
        ratingRepository.deleteByUser(userId);

        // Comments: lower each recipe's comment count, then delete.
        List<Object[]> commentCounts = commentRepository.countByRecipeForAuthor(userId);
        int comments = 0;
        for (Object[] row : commentCounts) {
            int count = ((Number) row[1]).intValue();
            recipeRepository.adjustCommentCount((Long) row[0], -count);
            comments += count;
        }
        commentRepository.deleteByAuthor(userId);

        // Recipes (their media files are removed once the transaction commits).
        List<RecipeEntity> recipes = recipeRepository.findByAuthorId(userId);
        List<String> mediaKeys = recipes.stream()
                .flatMap(r -> r.getMedia().stream())
                .map(RecipeMediaEntity::getStorageKey)
                .toList();
        recipeRepository.deleteAll(recipes);
        afterCommit(() -> mediaKeys.forEach(storage::delete));

        savedRecipeRepository.deleteByUser(userId);
        int follows = followRepository.deleteAllInvolving(userId);

        Summary summary = new Summary(scores.size(), comments, recipes.size(), follows);
        log.info("Purged content of user {}: {}", userId, summary);
        return summary;
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
