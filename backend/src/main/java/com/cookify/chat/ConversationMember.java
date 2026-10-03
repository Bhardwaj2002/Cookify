package com.cookify.chat;

import com.cookify.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/** A user's membership in a conversation, with how far they have read. */
@Entity
@Table(name = "conversation_members")
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember {

    @Embeddable
    public record Key(@Column(name = "conversation_id") Long conversationId, @Column(name = "user_id") Long userId)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;

    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    public ConversationMember(Conversation conversation, User user, Instant lastReadAt) {
        this.id = new Key(null, user.getId());
        this.conversation = conversation;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }
}
