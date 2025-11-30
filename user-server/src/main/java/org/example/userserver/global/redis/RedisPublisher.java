package org.example.userserver.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userserver.domain.stream.dto.RedisMessageDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisPubSubTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String streamId, String type, Object payloadDto) {
        String channel = "broadcast:" + streamId;
        try {
            String payload = objectMapper.writeValueAsString(payloadDto);
            RedisMessageDto redisMessage = RedisMessageDto.from(type, payload);
            redisPubSubTemplate.convertAndSend(channel, redisMessage);
        } catch (JsonProcessingException e) {
            log.error("Error serializing message payload for Redis publish", e);
            // Consider a more robust error handling strategy
            throw new RuntimeException("Error serializing message payload for Redis publish", e);
        }
    }
}
