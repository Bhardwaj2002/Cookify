package com.cookify.chat;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.Key> {

    @EntityGraph(attributePaths = {"conversation"})
    List<ConversationMember> findByIdUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ConversationMember> findByIdConversationId(Long conversationId);
}
