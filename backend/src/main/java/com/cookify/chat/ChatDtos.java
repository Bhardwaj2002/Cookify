package com.cookify.chat;

import com.cookify.recipe.dto.AuthorInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response bodies for chat (REST and WebSocket). */
public final class ChatDtos {

    private ChatDtos() {
    }

    public record StartConversationRequest(@NotBlank @Size(max = 30) String username) {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 2000) String body) {
    }

    public record MessageResponse(Long id, Long conversationId, AuthorInfo sender, String body, Instant createdAt) {

        public static MessageResponse from(Message m) {
            return new MessageResponse(m.getId(), m.getConversation().getId(), AuthorInfo.from(m.getSender()),
                    m.getBody(), m.getCreatedAt());
        }
    }

    /** Inbox row: the other person, the latest message and how many are unread. */
    public record ConversationResponse(Long id, AuthorInfo with, MessageResponse lastMessage, long unreadCount,
                                       Instant lastMessageAt) {
    }

    public record UnreadResponse(long unread) {
    }

    /** Pushed over WebSocket to {@code /user/queue/typing}. */
    public record TypingEvent(Long conversationId, String username) {
    }

    /** Sent by the browser to {@code /app/chat.typing}. */
    public record TypingRequest(Long conversationId) {
    }

    /** Sent by the browser to {@code /app/chat.send}. */
    public record SocketSendRequest(Long conversationId, String body) {
    }
}
