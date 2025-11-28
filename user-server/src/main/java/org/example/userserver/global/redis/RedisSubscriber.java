package org.example.userserver.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.userserver.domain.stream.dto.RedisMessageDto;
import org.example.userserver.domain.stream.dto.response.StreamUserCountUpdateDto;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final StreamService streamService;
    private final String streamUpdateChannel;

    public RedisSubscriber(ObjectMapper objectMapper,
                           StreamService streamService,
                           @Value("${app.redis-channel}") String streamUpdateChannel) {
        this.objectMapper = objectMapper;
        this.streamService = streamService;
        this.streamUpdateChannel = streamUpdateChannel;
    }

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
            RedisMessageDto messageDto = objectMapper.readValue(message.getBody(), RedisMessageDto.class);

            if (streamUpdateChannel.equals(messageDto.type())) {
                StreamUserCountUpdateDto updateDto = objectMapper.readValue(messageDto.payload(), StreamUserCountUpdateDto.class);
                streamService.notifyUserCountUpdate(updateDto);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process Redis message", e);
        }
    }
}