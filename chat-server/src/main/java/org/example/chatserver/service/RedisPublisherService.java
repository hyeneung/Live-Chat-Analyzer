package org.example.chatserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatserver.config.RedisConfig;
import org.example.chatserver.dto.RedisMessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisherService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String type, Object payloadDto) {
        try {
            String payload = objectMapper.writeValueAsString(payloadDto);
            RedisMessageDto redisMessage = RedisMessageDto.from(type, payload);
            String message = objectMapper.writeValueAsString(redisMessage);
            redisTemplate.convertAndSend(RedisConfig.CHANNEL_NAME, message);
            log.debug("Published message to Redis channel '{}': {}", RedisConfig.CHANNEL_NAME, message);
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for Redis publish", e);
        }
    }
}
