package com.cookify.social.saved;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, SavedRecipe.Key> {

    @EntityGraph(attributePaths = {"recipe", "recipe.author"})
    Page<SavedRecipe> findByIdUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SavedRecipe s WHERE s.id.userId = :userId")
    int deleteByUser(@Param("userId") Long userId);
}
