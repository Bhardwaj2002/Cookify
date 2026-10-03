package com.cookify.auth.dto;

import com.cookify.common.validation.ValidPassword;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @ValidPassword String password,
        @NotBlank String confirmPassword) {

    @JsonIgnore
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return Objects.equals(password, confirmPassword);
    }
}
