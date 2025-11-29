package org.example.chatserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatserver.service.RedisSubscriptionManager;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisSubscriptionManager redisSubscriptionManager;

    private static final Pattern STREAM_ID_PATTERN = Pattern.compile("/topic/stream/([^/]+)/.*");

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        log.info("Session {} subscribed to {}", sessionId, destination);

        if (destination == null) {
            log.warn("Subscription destination is null for session {}", sessionId);
            return;
        }

        Matcher matcher = STREAM_ID_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String streamId = matcher.group(1);
            redisSubscriptionManager.subscribe(streamId, sessionId);
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("Session {} disconnected", sessionId);

        // Iterate through all currently managed streams to find where this session was subscribed
        // This is a simplified approach; a more robust solution might track session-to-stream mappings more directly
        // in RedisSubscriptionManager or in a separate session management component.
        redisSubscriptionManager.getAllStreamIds().forEach(streamId ->
            redisSubscriptionManager.unsubscribe(streamId, sessionId)
        );
    }
}
