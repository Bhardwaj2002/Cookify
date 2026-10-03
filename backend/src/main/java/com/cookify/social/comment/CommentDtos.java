package com.cookify.social.comment;

import com.cookify.recipe.dto.AuthorInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response bodies for recipe comments. */
public final class CommentDtos {

    private CommentDtos() {
    }

    public record CommentRequest(@NotBlank @Size(max = 1000) String body) {
    }

    /** {@code canDelete}: the viewer wrote it, owns the recipe, or is an admin. */
    public record CommentResponse(Long id, String body, AuthorInfo author, Instant createdAt, boolean canDelete) {
    }
}
