package com.cookify.auth.dto;

import com.cookify.common.validation.ValidPassword;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public record SignupRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "may only contain letters, digits, '_' and '.'")
        String username,

        @NotBlank @Email @Size(max = 254)
        String email,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be 7-15 digits, optionally starting with +")
        String phone,

        @ValidPassword
        String password,

        @NotBlank
        String confirmPassword,

        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName) {

    /** Sign-up form asks for the password twice (see UI mockup). */
    @JsonIgnore
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return Objects.equals(password, confirmPassword);
    }
}
