package com.cookify.auth.entity;

/** Why a refresh token stopped being valid. */
public enum RevokeReason {
    /** Exchanged for a newer token by /refresh. */
    ROTATED,
    /** The user logged out. */
    LOGOUT,
    /** Mass revocation: password reset, ban, or detected token reuse. */
    REVOKED
}
