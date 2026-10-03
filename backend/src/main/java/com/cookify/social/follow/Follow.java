package com.cookify.social.follow;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** "Subscribe": {@code followerId} follows {@code followeeId}. */
@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor
public class Follow {

    @Embeddable
    public record Key(@Column(name = "follower_id") Long followerId, @Column(name = "followee_id") Long followeeId)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Follow(Long followerId, Long followeeId) {
        this.id = new Key(followerId, followeeId);
    }
}
