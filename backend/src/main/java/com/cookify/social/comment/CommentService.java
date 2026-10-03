package com.cookify.social.comment;

import com.cookify.common.exception.ForbiddenException;
import com.cookify.common.exception.NotFoundException;
import com.cookify.common.response.PageResponse;
import com.cookify.moderation.ModerationService;
import com.cookify.moderation.Warning;
import com.cookify.notification.ActivityEvents;
import org.springframework.context.ApplicationEventPublisher;
import com.cookify.recipe.dto.AuthorInfo;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.recipe.service.RecipeService;
import com.cookify.security.jwt.AuthUser;
import com.cookify.social.comment.CommentDtos.CommentResponse;
import com.cookify.user.entity.Role;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comments under a recipe, newest first. New comments are screened for abusive language. */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final UserService userService;
    private final ModerationService moderationService;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(Long recipeId, AuthUser viewer, int page, int size) {
        RecipeEntity recipe = recipeService.get(recipeId);
        return PageResponse.from(commentRepository.findByRecipeIdOrderByCreatedAtDescIdDesc(recipeId, PageRequest.of(page, size)),
                c -> toResponse(c, recipe, viewer));
    }

    @Transactional
    public CommentResponse add(Long recipeId, AuthUser caller, String body) {
        RecipeEntity recipe = recipeService.get(recipeId);
        moderationService.screen(caller, Warning.Reason.ABUSIVE_COMMENT, body);
        Comment comment = commentRepository.saveAndFlush(new Comment(recipe, userService.getById(caller.id()), body.trim()));
        recipeRepository.adjustCommentCount(recipeId, 1);
        events.publishEvent(new ActivityEvents.CommentAdded(comment.getId()));
        return toResponse(comment, recipe, caller);
    }

    /** The comment's author, the recipe's author or an admin may delete it. */
    @Transactional
    public void delete(Long commentId, AuthUser caller) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND", "Comment not found"));
        if (!canDelete(comment, comment.getRecipe(), caller)) {
            throw new ForbiddenException("NOT_COMMENT_OWNER", "You can't delete this comment");
        }
        Long recipeId = comment.getRecipe().getId();
        commentRepository.delete(comment);
        commentRepository.flush();
        recipeRepository.adjustCommentCount(recipeId, -1);
    }

    private static CommentResponse toResponse(Comment c, RecipeEntity recipe, AuthUser viewer) {
        return new CommentResponse(c.getId(), c.getBody(), AuthorInfo.from(c.getAuthor()), c.getCreatedAt(),
                canDelete(c, recipe, viewer));
    }

    private static boolean canDelete(Comment c, RecipeEntity recipe, AuthUser viewer) {
        return viewer != null && (viewer.role() == Role.ADMIN
                || viewer.id().equals(c.getAuthor().getId())
                || viewer.id().equals(recipe.getAuthor().getId()));
    }
}
