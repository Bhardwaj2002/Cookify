package com.cookify.moderation;

import com.cookify.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** A strike against a user for blocked content. Three warnings lead to a ban. */
@Entity
@Table(name = "warnings")
@Getter
@NoArgsConstructor
public class Warning {

    public enum Reason {
        ABUSIVE_COMMENT,
        ABUSIVE_RECIPE,
        ABUSIVE_MESSAGE,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Reason reason;

    @Column(name = "content_snippet", length = 200)
    private String contentSnippet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Warning(User user, Reason reason, String contentSnippet) {
        this.user = user;
        this.reason = reason;
        this.contentSnippet = contentSnippet == null || contentSnippet.length() <= 200
                ? contentSnippet : contentSnippet.substring(0, 197) + "...";
    }
}
