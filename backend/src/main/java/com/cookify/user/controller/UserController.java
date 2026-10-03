package com.cookify.user.controller;

import com.cookify.recipe.service.RecipeService;
import com.cookify.security.device.TrustedDeviceService;
import com.cookify.security.jwt.AuthUser;
import com.cookify.social.follow.FollowService;
import com.cookify.user.dto.PreferencesRequest;
import com.cookify.user.dto.PreferencesResponse;
import com.cookify.user.dto.PublicProfileResponse;
import com.cookify.user.dto.TrustedDeviceResponse;
import com.cookify.user.dto.TwoFactorUpdateRequest;
import com.cookify.user.dto.UpdateProfileRequest;
import com.cookify.user.dto.UserResponse;
import com.cookify.user.service.AvatarService;
import com.cookify.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;
    private final TrustedDeviceService trustedDeviceService;
    private final RecipeService recipeService;
    private final FollowService followService;

    // ---- Own account -------------------------------------------------------

    @Operation(summary = "Get the logged-in user's account")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return UserResponse.from(userService.getById(authUser.id()));
    }

    @Operation(summary = "Update profile details (username, name, phone, age, gender, bio, language)")
    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser authUser,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(authUser.id(), request));
    }

    @Operation(summary = "Upload a profile picture (JPEG, PNG, GIF or WebP, max 5 MB)")
    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(@AuthenticationPrincipal AuthUser authUser,
                                     @RequestPart("file") MultipartFile file) {
        return UserResponse.from(avatarService.upload(authUser.id(), file));
    }

    @Operation(summary = "Remove the profile picture")
    @DeleteMapping("/me/avatar")
    public UserResponse removeAvatar(@AuthenticationPrincipal AuthUser authUser) {
        return UserResponse.from(avatarService.remove(authUser.id()));
    }

    @Operation(summary = "Get dietary preferences")
    @GetMapping("/me/preferences")
    public PreferencesResponse preferences(@AuthenticationPrincipal AuthUser authUser) {
        return userService.getPreferences(authUser.id());
    }

    @Operation(summary = "Set dietary preferences (used for recommendations)")
    @PutMapping("/me/preferences")
    public PreferencesResponse updatePreferences(@AuthenticationPrincipal AuthUser authUser,
                                                 @Valid @RequestBody PreferencesRequest request) {
        return userService.updatePreferences(authUser.id(), request);
    }

    @Operation(summary = "Turn email two-factor authentication on or off (requires current password)")
    @PutMapping("/me/two-factor")
    public UserResponse setTwoFactor(@AuthenticationPrincipal AuthUser authUser,
                                     @Valid @RequestBody TwoFactorUpdateRequest request) {
        return UserResponse.from(userService.setTwoFactor(authUser.id(), request.enabled(), request.password()));
    }

    @Operation(summary = "List remembered devices that skip the 2FA code")
    @GetMapping("/me/devices")
    public List<TrustedDeviceResponse> devices(@AuthenticationPrincipal AuthUser authUser) {
        return trustedDeviceService.listActive(authUser.id()).stream().map(TrustedDeviceResponse::from).toList();
    }

    @Operation(summary = "Forget a remembered device")
    @DeleteMapping("/me/devices/{deviceId}")
    public ResponseEntity<Void> forgetDevice(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long deviceId) {
        trustedDeviceService.revoke(authUser.id(), deviceId);
        return ResponseEntity.noContent().build();
    }

    // ---- Public ------------------------------------------------------------

    @Operation(summary = "Public profile of any user (no login needed)")
    @SecurityRequirements
    @GetMapping("/{username}")
    public PublicProfileResponse publicProfile(@PathVariable String username, @AuthenticationPrincipal AuthUser viewer) {
        var user = userService.getPublicByUsername(username);
        return PublicProfileResponse.from(user, recipeService.countByAuthor(user.getId()),
                followService.followerCount(user.getId()), followService.followingCount(user.getId()),
                followService.isFollowing(viewer, user.getId()));
    }
}
