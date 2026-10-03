package com.cookify.social.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByRecipeIdAndUserId(Long recipeId, Long userId);

    /** (recipeId, score) pairs of a user's ratings, used to reverse them when the user is banned. */
    @Query("SELECT r.recipe.id, r.score FROM Rating r WHERE r.user.id = :userId")
    List<Object[]> findScoresByUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Rating r WHERE r.user.id = :userId")
    int deleteByUser(@Param("userId") Long userId);
}
