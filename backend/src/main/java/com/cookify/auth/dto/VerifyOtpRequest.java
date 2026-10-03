package com.cookify.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Login step 2 of 2. {@code rememberDevice} skips the code on this browser for the next 30 days. */
public record VerifyOtpRequest(
        @NotBlank @Size(max = 100) String challengeId,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit code") String code,
        Boolean rememberDevice) {

    /** Optional in the request body; missing means "don't remember". */
    public boolean remember() {
        return Boolean.TRUE.equals(rememberDevice);
    }
}
