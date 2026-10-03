package com.cookify.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "first_name", length = 60)
    private String firstName;

    @Column(name = "last_name", length = 60)
    private String lastName;

    private Short age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Storage key of the avatar file, used to delete it when replaced. Never exposed via the API. */
    @Column(name = "avatar_key", length = 300)
    private String avatarKey;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "twofa_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Column(name = "warnings_count", nullable = false)
    private int warningsCount;

    @Column(name = "banned_until")
    private Instant bannedUntil;

    @Column(name = "permanently_banned", nullable = false)
    private boolean permanentlyBanned;

    @Column(name = "ban_reason", length = 200)
    private String banReason;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "preferred_lang", nullable = false, length = 5)
    private String preferredLang = "en";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True if the account is permanently banned or a temporary ban is still running at {@code now}. */
    public boolean isBannedAt(Instant now) {
        return permanentlyBanned || (bannedUntil != null && bannedUntil.isAfter(now));
    }
}
