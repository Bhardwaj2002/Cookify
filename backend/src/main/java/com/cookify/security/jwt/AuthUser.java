package com.cookify.security.jwt;

import com.cookify.user.entity.Role;

import java.security.Principal;

/**
 * The authenticated caller, built from a verified access token.
 * Inject in controllers with {@code @AuthenticationPrincipal AuthUser user}.
 * {@link #getName()} is the user id, which WebSocket user destinations use for routing.
 */
public record AuthUser(Long id, String username, Role role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
