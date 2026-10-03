package com.cookify.social.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Follow.Key> {

    long countByIdFolloweeId(Long followeeId);

    long countByIdFollowerId(Long followerId);

    /** Everyone subscribed to {@code followeeId} (used for new-recipe emails). */
    List<Follow> findByIdFolloweeId(Long followeeId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id.followerId = :userId OR f.id.followeeId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}
