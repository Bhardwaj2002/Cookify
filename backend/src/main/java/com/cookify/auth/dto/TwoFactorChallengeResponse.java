package com.cookify.auth.dto;

/**
 * Login step 1 of 2: the password was correct, a code was emailed to {@code maskedEmail}.
 * The client must call {@code /api/auth/verify-otp} with {@code challengeId} and the code.
 */
public record TwoFactorChallengeResponse(boolean twoFactorRequired, String challengeId, String maskedEmail, long expiresIn) {

    public static TwoFactorChallengeResponse of(String challengeId, String maskedEmail, long expiresIn) {
        return new TwoFactorChallengeResponse(true, challengeId, maskedEmail, expiresIn);
    }
}
