package com.cookify.social;

import com.cookify.common.response.PageResponse;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.security.jwt.AuthUser;
import com.cookify.social.comment.CommentDtos.CommentRequest;
import com.cookify.social.comment.CommentDtos.CommentResponse;
import com.cookify.social.comment.CommentService;
import com.cookify.social.follow.FollowResponse;
import com.cookify.social.follow.FollowService;
import com.cookify.social.rating.RatingDtos.RatingRequest;
import com.cookify.social.rating.RatingDtos.RatingResponse;
import com.cookify.social.rating.RatingService;
import com.cookify.social.saved.SavedRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ratings, comments, saved recipes and subscriptions. */
@Tag(name = "Social")
@RestController
@RequiredArgsConstructor
public class SocialController {

    private final RatingService ratingService;
    private final CommentService commentService;
    private final SavedRecipeService savedRecipeService;
    private final FollowService followService;

    // ---- Ratings -------------------------------------------------------------------

    @Operation(summary = "Rate a recipe 1–5 (rating again replaces your previous score)")
    @PutMapping("/api/recipes/{id}/rating")
    public RatingResponse rate(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
                               @Valid @RequestBody RatingRequest request) {
        return ratingService.rate(id, caller, request.score());
    }

    @Operation(summary = "Remove your rating")
    @DeleteMapping("/api/recipes/{id}/rating")
    public RatingResponse unrate(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        return ratingService.remove(id, caller);
    }

    // ---- Comments ------------------------------------------------------------------

    @Operation(summary = "Comments on a recipe, newest first (public)")
    @GetMapping("/api/recipes/{id}/comments")
    public PageResponse<CommentResponse> comments(@PathVariable Long id, @AuthenticationPrincipal AuthUser viewer,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return commentService.list(id, viewer, page, size);
    }

    @Operation(summary = "Comment on a recipe")
    @PostMapping("/api/recipes/{id}/comments")
    public ResponseEntity<CommentResponse> comment(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
                                                   @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(id, caller, request.body()));
    }

    @Operation(summary = "Delete a comment (its author, the recipe author or an admin)")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal AuthUser caller) {
        commentService.delete(commentId, caller);
        return ResponseEntity.noContent().build();
    }

    // ---- Saved recipes -------------------------------------------------------------

    @Operation(summary = "Save (bookmark) a recipe")
    @PutMapping("/api/recipes/{id}/save")
    public ResponseEntity<Void> save(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        savedRecipeService.save(id, caller);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a recipe from saved")
    @DeleteMapping("/api/recipes/{id}/save")
    public ResponseEntity<Void> unsave(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        savedRecipeService.unsave(id, caller);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "My saved recipes, most recently saved first")
    @GetMapping("/api/users/me/saved")
    public PageResponse<RecipeSummaryResponse> saved(@AuthenticationPrincipal AuthUser caller,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return savedRecipeService.list(caller, page, size);
    }

    // ---- Subscriptions -------------------------------------------------------------

    @Operation(summary = "Subscribe to a cook")
    @PutMapping("/api/users/{username}/follow")
    public FollowResponse follow(@PathVariable String username, @AuthenticationPrincipal AuthUser caller) {
        return followService.follow(username, caller);
    }

    @Operation(summary = "Unsubscribe from a cook")
    @DeleteMapping("/api/users/{username}/follow")
    public FollowResponse unfollow(@PathVariable String username, @AuthenticationPrincipal AuthUser caller) {
        return followService.unfollow(username, caller);
    }
}
