package com.cookify.user.dto;

import com.cookify.user.entity.User;

import java.time.Instant;

/**
 * What anyone can see about a user (profile page). Never includes email, phone or account flags.
 * {@code following} tells whether the current viewer is subscribed (false for anonymous visitors).
 */
public record PublicProfileResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        Short age,
        Instant createdAt,
        long recipeCount,
        long followerCount,
        long followingCount,
        boolean following) {

    public static PublicProfileResponse from(User u, long recipeCount, long followerCount, long followingCount,
                                             boolean following) {
        return new PublicProfileResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                u.getAvatarUrl(), u.getBio(), u.getAge(), u.getCreatedAt(), recipeCount, followerCount, followingCount,
                following);
    }
}
