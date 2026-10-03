package com.cookify.moderation;

import com.cookify.auth.service.RefreshTokenService;
import com.cookify.notification.EmailTemplate;
import com.cookify.notification.Notifier;
import com.cookify.security.device.TrustedDeviceService;
import com.cookify.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Suspends (temporarily or permanently) and reinstates accounts. */
@Service
@RequiredArgsConstructor
public class BanService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC);

    private final ContentPurger contentPurger;
    private final RefreshTokenService refreshTokenService;
    private final TrustedDeviceService trustedDeviceService;
    private final Notifier notifier;
    private final Clock clock;

    /**
     * Bans {@code user}: {@code duration == null} means permanent. Signs them out everywhere,
     * removes their content and emails them.
     */
    @Transactional
    public void ban(User user, Duration duration, String reason) {
        Instant now = clock.instant();
        user.setPermanentlyBanned(duration == null);
        user.setBannedUntil(duration == null ? null : now.plus(duration));
        user.setBanReason(reason);
        user.setBannedAt(now);

        refreshTokenService.revokeAll(user.getId());
        trustedDeviceService.revokeAll(user.getId());
        contentPurger.purge(user.getId());

        String until = duration == null ? "permanently" : "until " + DATE.format(user.getBannedUntil());
        notifier.send(user, EmailTemplate.ACCOUNT_BANNED, Map.of("duration", until, "reason", reason));
    }

    /** Lifts a ban and clears the warning count so the user starts fresh. */
    @Transactional
    public void unban(User user) {
        user.setPermanentlyBanned(false);
        user.setBannedUntil(null);
        user.setBanReason(null);
        user.setBannedAt(null);
        user.setWarningsCount(0);
    }
}
