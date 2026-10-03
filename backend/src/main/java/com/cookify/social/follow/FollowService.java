package com.cookify.social.follow;

import com.cookify.common.exception.BadRequestException;
import com.cookify.notification.ActivityEvents;
import org.springframework.context.ApplicationEventPublisher;
import com.cookify.security.jwt.AuthUser;
import com.cookify.user.entity.User;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Subscribe" to another cook. Following twice / unfollowing when not following are no-ops. */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository repository;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Transactional
    public FollowResponse follow(String username, AuthUser caller) {
        User target = userService.getPublicByUsername(username);
        if (target.getId().equals(caller.id())) {
            throw new BadRequestException("CANNOT_FOLLOW_SELF", "You can't subscribe to yourself");
        }
        Follow.Key key = new Follow.Key(caller.id(), target.getId());
        if (!repository.existsById(key)) {
            repository.saveAndFlush(new Follow(caller.id(), target.getId()));
            events.publishEvent(new ActivityEvents.UserFollowed(caller.id(), target.getId()));
        }
        return new FollowResponse(true, repository.countByIdFolloweeId(target.getId()));
    }

    @Transactional
    public FollowResponse unfollow(String username, AuthUser caller) {
        User target = userService.getPublicByUsername(username);
        repository.deleteById(new Follow.Key(caller.id(), target.getId()));
        repository.flush();
        return new FollowResponse(false, repository.countByIdFolloweeId(target.getId()));
    }

    @Transactional(readOnly = true)
    public long followerCount(Long userId) {
        return repository.countByIdFolloweeId(userId);
    }

    @Transactional(readOnly = true)
    public long followingCount(Long userId) {
        return repository.countByIdFollowerId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(AuthUser viewer, Long userId) {
        return viewer != null && repository.existsById(new Follow.Key(viewer.id(), userId));
    }
}
