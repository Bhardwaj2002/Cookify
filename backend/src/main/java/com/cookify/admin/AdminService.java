package com.cookify.admin;

import com.cookify.admin.AdminDtos.AdminUserResponse;
import com.cookify.admin.AdminDtos.BanRequest;
import com.cookify.admin.AdminDtos.UserStatus;
import com.cookify.admin.AdminDtos.WarningResponse;
import com.cookify.common.exception.ForbiddenException;
import com.cookify.common.response.PageResponse;
import com.cookify.moderation.BanService;
import com.cookify.moderation.WarningRepository;
import com.cookify.user.entity.Role;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import com.cookify.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** User moderation for admins: search, warnings history, ban / unban. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WarningRepository warningRepository;
    private final UserService userService;
    private final BanService banService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String query, UserStatus status, int page, int size) {
        Instant now = clock.instant();
        Specification<User> spec = (root, q, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                where.add(cb.or(cb.like(cb.lower(root.get("username")), like), cb.like(cb.lower(root.get("email")), like)));
            }
            if (status == UserStatus.WARNED) {
                where.add(cb.gt(root.get("warningsCount"), 0));
            } else if (status == UserStatus.BANNED) {
                where.add(cb.or(cb.isTrue(root.get("permanentlyBanned")), cb.greaterThan(root.get("bannedUntil"), now)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        Sort sort = Sort.by(Sort.Order.desc("warningsCount"), Sort.Order.desc("createdAt"));
        return PageResponse.from(userRepository.findAll(spec, PageRequest.of(page, size, sort)),
                u -> AdminUserResponse.from(u, now));
    }

    @Transactional(readOnly = true)
    public List<WarningResponse> warnings(Long userId) {
        userService.getById(userId);
        return warningRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WarningResponse::from).toList();
    }

    @Transactional
    public AdminUserResponse ban(Long userId, BanRequest request) {
        User user = userService.getById(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("CANNOT_BAN_ADMIN", "Admins can't be banned");
        }
        banService.ban(user, request.isPermanent() ? null : Duration.ofDays(request.days()), request.reason().trim());
        return AdminUserResponse.from(user, clock.instant());
    }

    @Transactional
    public AdminUserResponse unban(Long userId) {
        User user = userService.getById(userId);
        banService.unban(user);
        return AdminUserResponse.from(user, clock.instant());
    }
}
