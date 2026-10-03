package com.cookify.user.dto;

import com.cookify.user.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Full replacement of the editable profile fields ({@code PUT /api/users/me}). Email is not editable here. */
public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "may only contain letters, digits, '_' and '.'")
        String username,

        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be 7-15 digits, optionally starting with +")
        String phone,

        @Min(13) @Max(120) Short age,

        Gender gender,

        @Size(max = 500) String bio,

        @NotBlank
        @Pattern(regexp = "^(en|hi|zh|ru|es|fr)$", message = "must be one of en, hi, zh, ru, es, fr")
        String preferredLang) {
}
