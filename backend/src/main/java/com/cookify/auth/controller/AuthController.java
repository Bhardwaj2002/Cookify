package com.cookify.auth.controller;

import com.cookify.auth.dto.AuthResponse;
import com.cookify.auth.dto.ForgotPasswordRequest;
import com.cookify.auth.dto.LoginRequest;
import com.cookify.auth.dto.ResetPasswordRequest;
import com.cookify.auth.dto.SignupRequest;
import com.cookify.auth.dto.TwoFactorChallengeResponse;
import com.cookify.auth.dto.VerifyOtpRequest;
import com.cookify.auth.service.AuthResult;
import com.cookify.auth.service.AuthService;
import com.cookify.auth.service.LoginOutcome;
import com.cookify.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthCookies cookies;

    @Operation(summary = "Create an account and log in")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request,
                                               @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authenticated(HttpStatus.CREATED, authService.signup(request, userAgent));
    }

    @Operation(summary = "Log in with username / email / phone and password",
            description = "Returns tokens, or — if 2FA is enabled and this device is not remembered — "
                    + "a challenge (twoFactorRequired=true) to complete with /verify-otp.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(oneOf = {AuthResponse.class, TwoFactorChallengeResponse.class})))
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   @CookieValue(name = AuthCookies.DEVICE, required = false) String deviceToken,
                                   @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return switch (authService.login(request, deviceToken, userAgent)) {
            case LoginOutcome.Authenticated a -> authenticated(HttpStatus.OK, a.result());
            case LoginOutcome.TwoFactorRequired c -> ResponseEntity.ok(c.challenge());
        };
    }

    @Operation(summary = "Complete a 2FA login with the emailed 6-digit code")
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                  @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authenticated(HttpStatus.OK, authService.verifyOtp(request, userAgent));
    }

    @Operation(summary = "Get a new access token using the refresh cookie")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken,
                                                @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authenticated(HttpStatus.OK, authService.refresh(refreshToken, userAgent));
    }

    @Operation(summary = "Log out: revoke the refresh token and clear the cookie (device stays remembered)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .build();
    }

    @Operation(summary = "Email a password reset link (always 202, whether or not the account exists)")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.identifier());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Set a new password using the token from the reset email; logs out all sessions")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> authenticated(HttpStatus status, AuthResult result) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString());
        if (result.trustedDeviceToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, cookies.device(result.trustedDeviceToken()).toString());
        }
        return builder.body(result.body());
    }
}
