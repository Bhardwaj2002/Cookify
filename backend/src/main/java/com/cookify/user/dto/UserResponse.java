package com.cookify.user.dto;

import com.cookify.user.entity.Gender;
import com.cookify.user.entity.Role;
import com.cookify.user.entity.User;

import java.time.Instant;

/** Private view of the logged-in user's own account (includes email and phone). */
public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName,
        Short age,
        Gender gender,
        String avatarUrl,
        String bio,
        Role role,
        boolean twoFactorEnabled,
        String preferredLang,
        Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getPhone(),
                u.getFirstName(), u.getLastName(), u.getAge(), u.getGender(), u.getAvatarUrl(), u.getBio(),
                u.getRole(), u.isTwoFactorEnabled(), u.getPreferredLang(), u.getCreatedAt());
    }
}
