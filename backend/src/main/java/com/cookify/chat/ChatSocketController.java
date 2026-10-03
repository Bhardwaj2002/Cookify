package com.cookify.chat;

import com.cookify.chat.ChatDtos.SocketSendRequest;
import com.cookify.chat.ChatDtos.TypingRequest;
import com.cookify.common.exception.ApiException;
import com.cookify.common.response.ErrorResponse;
import com.cookify.security.jwt.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/** STOMP endpoints: {@code /app/chat.send} and {@code /app/chat.typing}. Errors go to {@code /user/queue/errors}. */
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;

    @MessageMapping("chat.send")
    public void send(SocketSendRequest request, Principal principal) {
        chatService.send(user(principal), request.conversationId(), request.body());
    }

    @MessageMapping("chat.typing")
    public void typing(TypingRequest request, Principal principal) {
        chatService.typing(user(principal), request.conversationId());
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handle(ApiException ex) {
        return ErrorResponse.of(ex.getCode(), ex.getMessage(), "/app/chat");
    }

    private static AuthUser user(Principal principal) {
        return (AuthUser) ((Authentication) principal).getPrincipal();
    }
}
