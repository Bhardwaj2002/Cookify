package com.cookify.admin;

import com.cookify.moderation.Warning;
import com.cookify.user.entity.Role;
import com.cookify.user.entity.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/** Request/response bodies for the admin moderation API. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public enum UserStatus {
        ALL,
        WARNED,
        BANNED
    }

    public record AdminUserResponse(Long id, String username, String email, String firstName, String lastName,
                                    String avatarUrl, Role role, int warningsCount, boolean banned,
                                    boolean permanentlyBanned, Instant bannedUntil, String banReason, Instant createdAt) {

        public static AdminUserResponse from(User u, Instant now) {
            return new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(),
                    u.getAvatarUrl(), u.getRole(), u.getWarningsCount(), u.isBannedAt(now), u.isPermanentlyBanned(),
                    u.getBannedUntil(), u.getBanReason(), u.getCreatedAt());
        }
    }

    public record WarningResponse(Long id, Warning.Reason reason, String contentSnippet, Instant createdAt) {

        public static WarningResponse from(Warning w) {
            return new WarningResponse(w.getId(), w.getReason(), w.getContentSnippet(), w.getCreatedAt());
        }
    }

    /** Ban for {@code days} days, or for ever when {@code permanent} is true. */
    public record BanRequest(@Min(1) @Max(3650) Integer days, Boolean permanent, @NotBlank @Size(max = 200) String reason) {

        public boolean isPermanent() {
            return Boolean.TRUE.equals(permanent);
        }

        @JsonIgnore
        @AssertTrue(message = "Choose a number of days or a permanent ban")
        public boolean isDurationChosen() {
            return isPermanent() || days != null;
        }
    }
}
