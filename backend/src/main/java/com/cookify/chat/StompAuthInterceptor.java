package com.cookify.chat;

import com.cookify.security.jwt.AuthUser;
import com.cookify.security.jwt.AuthUserJwtConverter;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Authenticates WebSocket (STOMP) sessions with the same JWT as the REST API: the CONNECT frame
 * must carry {@code Authorization: Bearer <accessToken>}. Banned users are refused, and clients may
 * only subscribe to their own {@code /user/queue/*} destinations.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final AuthUserJwtConverter converter;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing access token");
            }
            AbstractAuthenticationToken auth;
            try {
                auth = converter.convert(jwtDecoder.decode(header.substring(7)));
            } catch (JwtException e) {
                throw new MessageDeliveryException("Invalid or expired access token");
            }
            AuthUser user = (AuthUser) auth.getPrincipal();
            if (userRepository.isBanned(user.id(), clock.instant())) {
                throw new MessageDeliveryException("This account has been suspended");
            }
            accessor.setUser(auth);
        } else if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            if (accessor.getUser() == null) {
                throw new MessageDeliveryException("Not authenticated");
            }
            String destination = accessor.getDestination();
            if (command == StompCommand.SUBSCRIBE && (destination == null || !destination.startsWith("/user/queue/"))) {
                throw new MessageDeliveryException("You can only subscribe to your own queues");
            }
        }
        return message;
    }
}
