package com.cookify.chat;

import com.cookify.chat.ChatDtos.ConversationResponse;
import com.cookify.chat.ChatDtos.MessageResponse;
import com.cookify.chat.ChatDtos.SendMessageRequest;
import com.cookify.chat.ChatDtos.StartConversationRequest;
import com.cookify.chat.ChatDtos.UnreadResponse;
import com.cookify.security.jwt.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat over REST. Live updates arrive over WebSocket: connect to {@code /ws} (STOMP, with an
 * {@code Authorization: Bearer …} CONNECT header) and subscribe to {@code /user/queue/messages}.
 */
@Tag(name = "Chat")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "My conversations, most recent first, with unread counts")
    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(@AuthenticationPrincipal AuthUser caller) {
        return chatService.list(caller);
    }

    @Operation(summary = "Open (or start) a conversation with a user")
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> start(@AuthenticationPrincipal AuthUser caller,
                                                      @Valid @RequestBody StartConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.start(caller, request.username()));
    }

    @Operation(summary = "Messages, oldest first; use beforeId to load older ones")
    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponse> messages(@AuthenticationPrincipal AuthUser caller, @PathVariable Long id,
                                          @RequestParam(required = false) Long beforeId,
                                          @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return chatService.messages(caller, id, beforeId, size);
    }

    @Operation(summary = "Send a message (also pushed live to both members)")
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> send(@AuthenticationPrincipal AuthUser caller, @PathVariable Long id,
                                                @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.send(caller, id, request.body()));
    }

    @Operation(summary = "Mark a conversation as read")
    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> read(@AuthenticationPrincipal AuthUser caller, @PathVariable Long id) {
        chatService.markRead(caller, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Total unread messages (header badge)")
    @GetMapping("/unread")
    public UnreadResponse unread(@AuthenticationPrincipal AuthUser caller) {
        return new UnreadResponse(chatService.unreadTotal(caller));
    }
}
