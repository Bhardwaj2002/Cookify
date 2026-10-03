package com.cookify.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Turning 2FA on or off requires re-entering the current password. */
public record TwoFactorUpdateRequest(@NotNull Boolean enabled, @NotBlank @Size(max = 72) String password) {
}
