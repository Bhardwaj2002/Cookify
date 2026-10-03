package com.cookify.social.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByRecipeIdOrderByCreatedAtDescIdDesc(Long recipeId, Pageable pageable);

    /** (recipeId, number of comments) for a user's comments, used to fix counts when the user is banned. */
    @Query("SELECT c.recipe.id, COUNT(c) FROM Comment c WHERE c.author.id = :userId GROUP BY c.recipe.id")
    List<Object[]> countByRecipeForAuthor(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.author.id = :userId")
    int deleteByAuthor(@Param("userId") Long userId);
}
