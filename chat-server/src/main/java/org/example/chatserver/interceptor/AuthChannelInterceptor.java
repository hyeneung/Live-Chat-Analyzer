package org.example.chatserver.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.chatserver.jwt.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Perform authentication only on a CONNECT command.
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }

        return message;
    }

    /**
     * Extracts and validates the JWT token from the STOMP headers to authenticate a user.
     * @param accessor The StompHeaderAccessor to access message headers.
     */
    private void authenticate(StompHeaderAccessor accessor) {
        // Extract the "Bearer " token from the "Authorization" header.
        Optional<String> tokenOptional = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));

        if (tokenOptional.isEmpty()) {
            log.warn("WebSocket connection failed: Missing or invalid 'Authorization' header.");
            return;
        }

        String token = tokenOptional.get();

        // Validate the token.
        if (jwtUtil.validateToken(token)) {
            String userId = jwtUtil.getUserIdFromToken(token);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.singleton(new SimpleGrantedAuthority("USER"))
            );

            // Set the user for the WebSocket session.
            accessor.setUser(authentication);
            log.info("User '{}' successfully connected via WebSocket.", userId);
        } else {
            log.warn("WebSocket connection failed: Invalid JWT token.");
        }
    }
}