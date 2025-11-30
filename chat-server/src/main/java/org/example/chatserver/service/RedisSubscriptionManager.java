package org.example.chatserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriptionManager {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final MessageListenerAdapter messageListenerAdapter;
    private final ConcurrentHashMap<String, Set<String>> streamSubscribers = new ConcurrentHashMap<>();

    public void subscribe(String streamId, String sessionId) {
        synchronized (streamId.intern()) {
            // Ensure the set is thread-safe for multiple sessions subscribing to the same streamId
            Set<String> sessions = streamSubscribers.computeIfAbsent(streamId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
            sessions.add(sessionId);
            log.info("Session {} subscribed to stream {}. Total subscribers for stream {}: {}", sessionId, streamId, streamId, sessions.size());

            if (sessions.size() == 1) {
                // Only add listener if this is the first subscriber for this stream on this server instance
                String channelName = "broadcast:" + streamId;
                redisMessageListenerContainer.addMessageListener(messageListenerAdapter, new ChannelTopic(channelName));
                log.info("Server now listening to Redis channel: {}", channelName);
            }
        }
    }

    public void unsubscribe(String streamId, String sessionId) {
        synchronized (streamId.intern()) {
            Set<String> sessions = streamSubscribers.get(streamId);
            if (sessions != null) {
                boolean removed = sessions.remove(sessionId);
                if (removed) {
                    log.info("Session {} unsubscribed from stream {}. Remaining subscribers for stream {}: {}", sessionId, streamId, streamId, sessions.size());

                    if (sessions.isEmpty()) {
                        // No more subscribers for this stream on this server instance, remove listener
                        String channelName = "broadcast:" + streamId;
                        redisMessageListenerContainer.removeMessageListener(messageListenerAdapter, new ChannelTopic(channelName));
                        streamSubscribers.remove(streamId); // Clean up empty entry
                        log.info("Server stopped listening to Redis channel: {}", channelName);
                    }
                } else {
                    log.warn("Attempted to unsubscribe session {} from stream {} but session was not found in active subscribers.", sessionId, streamId);
                }
            } else {
                log.warn("Attempted to unsubscribe from stream {} but no subscribers were found for this stream on this server instance.", streamId);
            }
        }
    }

    public Set<String> getAllStreamIds() {
        return Collections.unmodifiableSet(streamSubscribers.keySet());
    }
}
