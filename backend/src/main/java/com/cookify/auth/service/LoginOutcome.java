package com.cookify.auth.service;

import com.cookify.auth.dto.TwoFactorChallengeResponse;

/** Result of a password login: either fully authenticated, or a 2FA code is still needed. */
public sealed interface LoginOutcome {

    record Authenticated(AuthResult result) implements LoginOutcome {
    }

    record TwoFactorRequired(TwoFactorChallengeResponse challenge) implements LoginOutcome {
    }
}
