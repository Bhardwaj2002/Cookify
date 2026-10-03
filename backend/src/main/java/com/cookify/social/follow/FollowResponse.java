package com.cookify.social.follow;

/** State after subscribing / unsubscribing. */
public record FollowResponse(boolean following, long followerCount) {
}
