package com.cookify.chat;

import com.cookify.chat.ChatDtos.ConversationResponse;
import com.cookify.chat.ChatDtos.MessageResponse;
import com.cookify.chat.ChatDtos.TypingEvent;
import com.cookify.common.exception.BadRequestException;
import com.cookify.common.exception.NotFoundException;
import com.cookify.moderation.ModerationService;
import com.cookify.moderation.Warning;
import com.cookify.recipe.dto.AuthorInfo;
import com.cookify.security.jwt.AuthUser;
import com.cookify.user.entity.User;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One-to-one chat. Messages are stored, then pushed live to both members over WebSocket
 * ({@code /user/queue/messages}) once the transaction commits.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    static final String MESSAGES_QUEUE = "/queue/messages";
    static final String TYPING_QUEUE = "/queue/typing";

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ModerationService moderationService;
    private final SimpMessagingTemplate messaging;
    private final Clock clock;

    /** Opens the existing conversation with {@code username}, or starts one. */
    @Transactional
    public ConversationResponse start(AuthUser caller, String username) {
        User other = userService.getPublicByUsername(username);
        if (other.getId().equals(caller.id())) {
            throw new BadRequestException("CANNOT_CHAT_SELF", "You can't message yourself");
        }
        Conversation conversation = conversationRepository.findBetween(caller.id(), other.getId()).orElseGet(() -> {
            Instant now = clock.instant();
            Conversation created = new Conversation();
            created.getMembers().add(new ConversationMember(created, userService.getById(caller.id()), now));
            created.getMembers().add(new ConversationMember(created, other, now));
            return conversationRepository.saveAndFlush(created);
        });
        return summary(conversation.getId(), caller.id());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(AuthUser caller) {
        List<ConversationResponse> result = new ArrayList<>();
        for (ConversationMember membership : memberRepository.findByIdUserId(caller.id())) {
            result.add(summary(membership.getConversation().getId(), caller.id()));
        }
        result.sort(Comparator.comparing(ConversationResponse::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /** A page of messages, oldest first; pass the oldest id you have as {@code beforeId} to load earlier ones. */
    @Transactional(readOnly = true)
    public List<MessageResponse> messages(AuthUser caller, Long conversationId, Long beforeId, int size) {
        requireMember(conversationId, caller.id());
        PageRequest page = PageRequest.of(0, size);
        List<Message> newestFirst = beforeId == null
                ? messageRepository.findByConversationIdOrderByIdDesc(conversationId, page)
                : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, beforeId, page);
        return newestFirst.reversed().stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public MessageResponse send(AuthUser caller, Long conversationId, String body) {
        ConversationMember me = requireMember(conversationId, caller.id());
        String text = body == null ? "" : body.trim();
        if (text.isEmpty() || text.length() > 2000) {
            throw new BadRequestException("INVALID_MESSAGE", "Messages must be 1–2000 characters");
        }
        moderationService.screen(caller, Warning.Reason.ABUSIVE_MESSAGE, text);

        Conversation conversation = me.getConversation();
        Message message = messageRepository.saveAndFlush(new Message(conversation, me.getUser(), text));
        conversation.setLastMessageAt(message.getCreatedAt());
        me.setLastReadAt(message.getCreatedAt());

        MessageResponse response = MessageResponse.from(message);
        List<Long> recipients = memberRepository.findByIdConversationId(conversationId).stream()
                .map(m -> m.getUser().getId()).toList();
        afterCommit(() -> recipients.forEach(id -> messaging.convertAndSendToUser(String.valueOf(id), MESSAGES_QUEUE, response)));
        return response;
    }

    @Transactional
    public void markRead(AuthUser caller, Long conversationId) {
        requireMember(conversationId, caller.id()).setLastReadAt(clock.instant());
    }

    @Transactional(readOnly = true)
    public long unreadTotal(AuthUser caller) {
        return memberRepository.findByIdUserId(caller.id()).stream()
                .mapToLong(m -> messageRepository.countUnread(m.getConversation().getId(), caller.id(), m.getLastReadAt()))
                .sum();
    }

    /** "… is typing" hint for the other member (not stored). */
    @Transactional(readOnly = true)
    public void typing(AuthUser caller, Long conversationId) {
        requireMember(conversationId, caller.id());
        TypingEvent event = new TypingEvent(conversationId, caller.username());
        memberRepository.findByIdConversationId(conversationId).stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(caller.id()))
                .forEach(id -> messaging.convertAndSendToUser(String.valueOf(id), TYPING_QUEUE, event));
    }

    // ---- Internals --------------------------------------------------------------------

    private ConversationMember requireMember(Long conversationId, Long userId) {
        return memberRepository.findById(new ConversationMember.Key(conversationId, userId))
                .orElseThrow(() -> new NotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
    }

    private ConversationResponse summary(Long conversationId, Long userId) {
        List<ConversationMember> members = memberRepository.findByIdConversationId(conversationId);
        ConversationMember me = members.stream().filter(m -> m.getUser().getId().equals(userId)).findFirst().orElseThrow();
        User other = members.stream().map(ConversationMember::getUser).filter(u -> !u.getId().equals(userId))
                .findFirst().orElseThrow();
        MessageResponse last = messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                .map(MessageResponse::from).orElse(null);
        long unread = messageRepository.countUnread(conversationId, userId, me.getLastReadAt());
        return new ConversationResponse(conversationId, AuthorInfo.from(other), last, unread,
                last == null ? null : last.createdAt());
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
