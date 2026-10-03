package com.cookify.moderation;

import com.cookify.common.exception.ApiException;
import com.cookify.common.exception.ForbiddenException;
import com.cookify.security.jwt.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Screens user-written text before it is saved. Abusive text is rejected and the author gets a
 * warning (emailed); the configured number of warnings bans the account.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ProfanityFilter filter;
    private final WarningRecorder warningRecorder;

    /**
     * @throws ApiException {@code 400 CONTENT_BLOCKED} with the warning count, or
     *                      {@code 403 ACCOUNT_BANNED} if this warning reached the limit
     */
    public void screen(AuthUser author, Warning.Reason reason, String... texts) {
        Optional<String> offending = Arrays.stream(texts)
                .filter(Objects::nonNull)
                .filter(text -> !filter.isClean(text))
                .findFirst();
        if (offending.isEmpty()) {
            return;
        }
        WarningRecorder.Outcome outcome = warningRecorder.record(author.id(), reason, offending.get());
        if (outcome.banned()) {
            throw new ForbiddenException("ACCOUNT_BANNED",
                    "Your account has been suspended after " + outcome.max() + " warnings for abusive content.");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "CONTENT_BLOCKED",
                "Please keep Cookify friendly — that language isn't allowed. This is warning %d of %d."
                        .formatted(outcome.warnings(), outcome.max()));
    }
}
