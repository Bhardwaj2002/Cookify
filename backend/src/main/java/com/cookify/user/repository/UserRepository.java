package com.cookify.user.repository;

import com.cookify.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE lower(u.username) = lower(:username)")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE lower(u.username) = lower(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);

    /** Login lookup: the identifier may be a username, an email or a phone number. */
    @Query("""
            SELECT u FROM User u
            WHERE lower(u.username) = lower(:identifier)
               OR lower(u.email) = lower(:identifier)
               OR u.phone = :identifier
            """)
    Optional<User> findByLoginIdentifier(@Param("identifier") String identifier);

    /** Cheap per-request ban check (see BanCheckFilter). */
    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            WHERE u.id = :id AND (u.permanentlyBanned = true OR u.bannedUntil > :now)
            """)
    boolean isBanned(@Param("id") Long id, @Param("now") Instant now);
}
