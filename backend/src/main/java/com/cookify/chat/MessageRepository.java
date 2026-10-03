package com.cookify.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Newest first (cursor paging for "load older"). */
    @EntityGraph(attributePaths = {"sender"})
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long beforeId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Optional<Message> findFirstByConversationIdOrderByIdDesc(Long conversationId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.createdAt > :since
            """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("since") Instant since);
}
