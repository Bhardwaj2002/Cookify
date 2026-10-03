package com.cookify.security.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findByChallengeHash(String challengeHash);

    /** A new login attempt supersedes any earlier pending challenge. */
    @Modifying
    @Query("UPDATE OtpCode o SET o.usedAt = :now WHERE o.user.id = :userId AND o.usedAt IS NULL")
    int invalidatePending(@Param("userId") Long userId, @Param("now") Instant now);
}
