package com.cookify.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Absolute links to frontend pages, for use in emails. */
@Component
public class LinkBuilder {

    private final String frontendUrl;

    public LinkBuilder(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public String recipe(Long id) {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/pages/recipe.html").queryParam("id", id).toUriString();
    }

    public String recipeComments(Long id) {
        return recipe(id) + "#comments";
    }

    public String profile(String username) {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/pages/profile.html").queryParam("u", username).toUriString();
    }
}
