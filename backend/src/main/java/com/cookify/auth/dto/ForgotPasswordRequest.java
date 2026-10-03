package com.cookify.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code identifier} may be a username, email or phone number. */
public record ForgotPasswordRequest(@NotBlank @Size(max = 254) String identifier) {
}
