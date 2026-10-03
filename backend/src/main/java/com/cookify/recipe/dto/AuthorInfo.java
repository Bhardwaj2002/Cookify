package com.cookify.recipe.dto;

import com.cookify.user.entity.User;

/** Minimal public author details shown on recipe cards and pages. */
public record AuthorInfo(Long id, String username, String firstName, String lastName, String avatarUrl) {

    public static AuthorInfo from(User u) {
        return new AuthorInfo(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getAvatarUrl());
    }
}
