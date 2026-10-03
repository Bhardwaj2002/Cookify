package com.cookify.security.device;

import com.cookify.common.exception.NotFoundException;
import com.cookify.common.util.SecureTokens;
import com.cookify.security.otp.TwoFactorProperties;
import com.cookify.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** "Remember this device": an opaque cookie token whose hash is stored per user. */
@Service
@RequiredArgsConstructor
public class TrustedDeviceService {

    private final TrustedDeviceRepository repository;
    private final TwoFactorProperties props;
    private final Clock clock;

    /** True if the cookie token belongs to this user and has not expired. Records the use. */
    @Transactional
    public boolean isTrusted(User user, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        Instant now = clock.instant();
        return repository.findByTokenHash(SecureTokens.sha256(rawToken))
                .filter(d -> d.getUser().getId().equals(user.getId()))
                .filter(d -> d.getExpiresAt().isAfter(now))
                .map(d -> {
                    d.setLastUsedAt(now);
                    return true;
                })
                .orElse(false);
    }

    /** Registers the current browser and returns the raw token for the cookie. */
    @Transactional
    public String trust(User user, String userAgent) {
        String raw = SecureTokens.generate();
        TrustedDevice device = new TrustedDevice();
        device.setUser(user);
        device.setTokenHash(SecureTokens.sha256(raw));
        device.setUserAgent(userAgent == null || userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255));
        device.setExpiresAt(clock.instant().plus(props.trustedDeviceTtl()));
        repository.save(device);
        return raw;
    }

    @Transactional(readOnly = true)
    public List<TrustedDevice> listActive(Long userId) {
        return repository.findActiveByUser(userId, clock.instant());
    }

    @Transactional
    public void revoke(Long userId, Long deviceId) {
        TrustedDevice device = repository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new NotFoundException("DEVICE_NOT_FOUND", "Device not found"));
        repository.delete(device);
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
