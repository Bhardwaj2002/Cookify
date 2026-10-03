package com.cookify.social.rating;

import com.cookify.common.exception.BadRequestException;
import com.cookify.notification.ActivityEvents;
import org.springframework.context.ApplicationEventPublisher;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.recipe.service.RecipeService;
import com.cookify.security.jwt.AuthUser;
import com.cookify.social.rating.RatingDtos.RatingResponse;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 1–5 star ratings. One per user and recipe; rating again replaces the old score. The recipe's
 * average is adjusted atomically in SQL ({@link RecipeRepository#applyRating}).
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Transactional
    public RatingResponse rate(Long recipeId, AuthUser caller, int score) {
        RecipeEntity recipe = recipeService.get(recipeId);
        if (recipe.getAuthor().getId().equals(caller.id())) {
            throw new BadRequestException("CANNOT_RATE_OWN", "You can't rate your own recipe");
        }

        Optional<Rating> existing = ratingRepository.findByRecipeIdAndUserId(recipeId, caller.id());
        if (existing.isPresent()) {
            Rating rating = existing.get();
            int delta = score - rating.getScore();
            rating.setScore((short) score);
            ratingRepository.saveAndFlush(rating);
            recipeRepository.applyRating(recipeId, delta, 0);
        } else {
            ratingRepository.saveAndFlush(new Rating(recipe, userService.getById(caller.id()), (short) score));
            recipeRepository.applyRating(recipeId, score, 1);
            // Only a first rating is announced, so changing your mind doesn't spam the author.
            events.publishEvent(new ActivityEvents.RecipeRated(recipeId, caller.id(), score));
        }
        return totals(recipeId, score);
    }

    @Transactional
    public RatingResponse remove(Long recipeId, AuthUser caller) {
        recipeService.get(recipeId);
        ratingRepository.findByRecipeIdAndUserId(recipeId, caller.id()).ifPresent(rating -> {
            int score = rating.getScore();
            ratingRepository.delete(rating);
            ratingRepository.flush();
            recipeRepository.applyRating(recipeId, -score, -1);
        });
        return totals(recipeId, null);
    }

    private RatingResponse totals(Long recipeId, Integer myRating) {
        RecipeEntity fresh = recipeService.get(recipeId);
        return new RatingResponse(fresh.getAvgRating(), fresh.getRatingCount(), myRating);
    }
}
