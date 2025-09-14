package org.example.userserver.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.stream.dto.response.StreamUserCountUpdateDto;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RedisSubscriber is a message listener that processes messages from Redis channels.
 * It is responsible for handling real-time updates, specifically for notifying
 * stream user count changes.
 */
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    /**
     * Callback method executed when a message is received from a Redis channel.
     * Deserializes the message body to extract the stream ID and then
     * notifies the StreamService to update the user count for that stream.
     *
     * @param message The received message, containing the stream ID in its body.
     * @param pattern The pattern that matched the channel (if using pattern-matching subscriptions).
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            StreamUserCountUpdateDto dto = objectMapper.readValue(message.getBody(), StreamUserCountUpdateDto.class);
            streamService.notifyUserCountUpdate(dto);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process Redis message", e);
        }
    }
}