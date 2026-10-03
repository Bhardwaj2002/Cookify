package com.cookify.user.service;

import com.cookify.common.exception.BadRequestException;
import com.cookify.common.exception.ConflictException;
import com.cookify.common.exception.NotFoundException;
import com.cookify.security.device.TrustedDeviceService;
import com.cookify.user.dto.PreferencesRequest;
import com.cookify.user.dto.PreferencesResponse;
import com.cookify.user.dto.UpdateProfileRequest;
import com.cookify.user.entity.User;
import com.cookify.user.entity.UserPreference;
import com.cookify.user.repository.UserPreferenceRepository;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrustedDeviceService trustedDeviceService;
    private final Clock clock;

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    /** Public profile lookup; banned accounts are hidden. */
    public User getPublicByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .filter(u -> !u.isBannedAt(clock.instant()))
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User user = getById(userId);
        String username = req.username().trim();
        String phone = blankToNull(req.phone());

        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("USERNAME_TAKEN", "This username is taken, try again");
        }
        if (phone != null && !phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_TAKEN", "An account with this phone number already exists");
        }

        user.setUsername(username);
        user.setFirstName(blankToNull(req.firstName()));
        user.setLastName(blankToNull(req.lastName()));
        user.setPhone(phone);
        user.setAge(req.age());
        user.setGender(req.gender());
        user.setBio(blankToNull(req.bio()));
        user.setPreferredLang(req.preferredLang());
        return user;
    }

    public PreferencesResponse getPreferences(Long userId) {
        return preferenceRepository.findById(userId)
                .map(PreferencesResponse::from)
                .orElse(PreferencesResponse.NONE);
    }

    @Transactional
    public PreferencesResponse updatePreferences(Long userId, PreferencesRequest req) {
        UserPreference prefs = preferenceRepository.findById(userId)
                .orElseGet(() -> new UserPreference(getById(userId)));
        prefs.setDietType(req.dietType());
        prefs.setMaxCalories(req.maxCalories());
        prefs.setMinProtein(req.minProtein());
        return PreferencesResponse.from(preferenceRepository.save(prefs));
    }

    /** Enables or disables email 2FA after re-checking the password. Disabling forgets all remembered devices. */
    @Transactional
    public User setTwoFactor(Long userId, boolean enabled, String currentPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("INVALID_PASSWORD", "Current password is incorrect");
        }
        user.setTwoFactorEnabled(enabled);
        if (!enabled) {
            trustedDeviceService.revokeAll(userId);
        }
        return user;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
