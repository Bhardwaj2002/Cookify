package com.cookify.support;

import com.cookify.auth.controller.AuthCookies;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Shared request helpers for auth-related integration tests. */
public final class AuthApi {

    public static final String PASSWORD = "Sup3rSecret!";

    private final MockMvc mockMvc;

    public AuthApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public ResultActions signup(String username, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","email":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(username, email, PASSWORD, PASSWORD)));
    }

    public ResultActions login(String identifier, String password, Cookie... cookies) throws Exception {
        var request = post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"identifier":"%s","password":"%s"}
                """.formatted(identifier, password));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request);
    }

    public ResultActions verifyOtp(String challengeId, String code, boolean rememberDevice) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content("""
                {"challengeId":"%s","code":"%s","rememberDevice":%s}
                """.formatted(challengeId, code, rememberDevice)));
    }

    public ResultActions setTwoFactor(String accessToken, boolean enabled, String password) throws Exception {
        return mockMvc.perform(put("/api/users/me/two-factor")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":%s,\"password\":\"%s\"}".formatted(enabled, password)));
    }

    public static String json(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    public static Cookie refreshCookie(MvcResult result) {
        return cookie(result, AuthCookies.REFRESH);
    }

    public static Cookie deviceCookie(MvcResult result) {
        return cookie(result, AuthCookies.DEVICE);
    }

    private static Cookie cookie(MvcResult result, String name) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
