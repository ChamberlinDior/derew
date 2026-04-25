package com.oviro.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

/**
 * Validates the JWT sent in the STOMP CONNECT Authorization header and sets
 * the user principal so that SimpMessagingTemplate.convertAndSendToUser()
 * routes messages to the right subscriber.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT sans token JWT – connexion refusée");
            throw new IllegalArgumentException("Token JWT manquant dans les headers STOMP CONNECT");
        }

        String token = authHeader.substring(7);
        try {
            jwtUtil.validateToken(token);
            UUID userId = jwtUtil.extractUserId(token);
            accessor.setUser(new StompPrincipal(userId.toString()));
            log.debug("WebSocket CONNECT authentifié pour userId={}", userId);
        } catch (Exception e) {
            log.warn("WebSocket CONNECT – token invalide: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT invalide ou expiré");
        }

        return message;
    }

    /** Lightweight Principal wrapping the user UUID as its name. */
    public record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
