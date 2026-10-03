package com.cookify.moderation;

import com.cookify.notification.EmailTemplate;
import com.cookify.notification.Notifier;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Records a warning in its own transaction, so the strike is kept even though the request that
 * triggered it fails. Reaching the limit bans the account.
 */
@Component
@RequiredArgsConstructor
public class WarningRecorder {

    private final UserRepository userRepository;
    private final WarningRepository warningRepository;
    private final BanService banService;
    private final Notifier notifier;
    private final ModerationProperties props;

    public record Outcome(int warnings, int max, boolean banned) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome record(Long userId, Warning.Reason reason, String snippet) {
        User user = userRepository.findById(userId).orElseThrow();
        warningRepository.save(new Warning(user, reason, snippet));
        user.setWarningsCount(user.getWarningsCount() + 1);
        int count = user.getWarningsCount();
        int max = props.warningsBeforeBan();

        if (count >= max) {
            banService.ban(user, props.autoBanDuration(), "Received " + max + " warnings for abusive content");
            return new Outcome(count, max, true);
        }
        String what = switch (reason) {
            case ABUSIVE_RECIPE -> "recipe";
            case ABUSIVE_MESSAGE -> "chat message";
            default -> "comment";
        };
        notifier.send(user, EmailTemplate.ACCOUNT_WARNING,
                Map.of("what", what, "snippet", snippet == null ? "" : snippet, "count", count, "max", max));
        return new Outcome(count, max, false);
    }
}
