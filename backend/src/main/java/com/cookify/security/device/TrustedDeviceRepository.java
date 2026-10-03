package com.cookify.security.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByTokenHash(String tokenHash);

    @Query("SELECT d FROM TrustedDevice d WHERE d.user.id = :userId AND d.expiresAt > :now ORDER BY d.createdAt DESC")
    List<TrustedDevice> findActiveByUser(@Param("userId") Long userId, @Param("now") Instant now);

    Optional<TrustedDevice> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM TrustedDevice d WHERE d.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
