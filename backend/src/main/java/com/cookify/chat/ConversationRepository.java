package com.cookify.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** The existing one-to-one conversation between two users, if any. */
    @Query("""
            SELECT c FROM Conversation c
            WHERE EXISTS (SELECT 1 FROM ConversationMember m WHERE m.conversation = c AND m.user.id = :a)
              AND EXISTS (SELECT 1 FROM ConversationMember m WHERE m.conversation = c AND m.user.id = :b)
            """)
    Optional<Conversation> findBetween(@Param("a") Long a, @Param("b") Long b);
}
