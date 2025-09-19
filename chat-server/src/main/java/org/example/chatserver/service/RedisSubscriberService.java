package org.example.chatserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatserver.dto.StreamUserCountUpdateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriberService {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void receiveMessage(String message) {
        try {
            log.debug("Received message from Redis: {}", message);

            StreamUserCountUpdateDto updateDto = objectMapper.readValue(message, StreamUserCountUpdateDto.class);
            String streamId = updateDto.streamId();

            String destination = org.example.chatserver.config.WebSocketConstants.TOPIC_PREFIX + "/stream/" + streamId + "/user-count";
            messagingTemplate.convertAndSend(destination, updateDto);

            log.debug("Forwarded user count update to {}: {}", destination, updateDto);
        } catch (Exception e) {
            log.error("Error processing message from Redis: {}", message, e);
        }
    }
}
