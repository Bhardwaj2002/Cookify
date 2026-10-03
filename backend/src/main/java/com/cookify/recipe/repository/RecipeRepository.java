package com.cookify.recipe.repository;

import com.cookify.recipe.entity.RecipeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long>, JpaSpecificationExecutor<RecipeEntity> {

    @EntityGraph(attributePaths = {"author"})
    Page<RecipeEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    long countByAuthorId(Long authorId);

    java.util.List<RecipeEntity> findByAuthorId(Long authorId);

    /**
     * Atomically applies a rating change: {@code scoreDelta} to the sum and {@code countDelta}
     * (+1 new, 0 changed, -1 removed) to the count, then recomputes the average.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE recipes SET
                rating_sum   = rating_sum + :scoreDelta,
                rating_count = rating_count + :countDelta,
                avg_rating   = CASE WHEN rating_count + :countDelta > 0
                                    THEN ROUND((rating_sum + :scoreDelta)::numeric / (rating_count + :countDelta), 2)
                                    ELSE 0 END
            WHERE id = :id""")
    int applyRating(@Param("id") Long id, @Param("scoreDelta") int scoreDelta, @Param("countDelta") int countDelta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RecipeEntity r SET r.commentCount = r.commentCount + :delta WHERE r.id = :id")
    int adjustCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /** Atomic increment so concurrent viewers never lose a count. */
    @Modifying
    @Query("UPDATE RecipeEntity r SET r.views = r.views + 1 WHERE r.id = :id")
    int incrementViews(@Param("id") Long id);
}
