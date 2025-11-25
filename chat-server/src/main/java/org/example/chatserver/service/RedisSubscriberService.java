package org.example.chatserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatserver.config.WebSocketConstants;
import org.example.chatserver.dto.*;
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
            RedisMessageDto redisMessage = objectMapper.readValue(message, RedisMessageDto.class);
            String type = redisMessage.type();
            String payload = redisMessage.payload();

            switch (type) {
                case "analysis":
                    AnalysisResultDto analysisDto = objectMapper.readValue(payload, AnalysisResultDto.class);
                    String analysisDestination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + analysisDto.streamId() + "/analysis";
                    messagingTemplate.convertAndSend(analysisDestination, analysisDto);
                    log.debug("Forwarded analysis result to {}: {}", analysisDestination, analysisDto);
                    break;
                case "chat":
                    ChatMessageDto chatDto = objectMapper.readValue(payload, ChatMessageDto.class);
                    String chatDestination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + chatDto.streamId() + "/message";
                    messagingTemplate.convertAndSend(chatDestination, chatDto);
                    log.debug("Forwarded chat message to {}: {}", chatDestination, chatDto);
                    break;
                case "summary":
                    SummaryResultDto summaryDto = objectMapper.readValue(payload, SummaryResultDto.class);
                    String summaryDestination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + summaryDto.streamId() + "/summary";
                    messagingTemplate.convertAndSend(summaryDestination, summaryDto);
                    log.debug("Forwarded summary result to {}: {}", summaryDestination, summaryDto);
                    break;
                case "stream-update":
                    StreamUserCountUpdateDto userCountDto = objectMapper.readValue(payload, StreamUserCountUpdateDto.class);
                    String userCountDestination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + userCountDto.streamId() + "/user-count";
                    messagingTemplate.convertAndSend(userCountDestination, userCountDto);
                    log.debug("Forwarded user count update to {}: {}", userCountDestination, userCountDto);
                    break;
                default:
                    log.warn("Received unknown message type from Redis: {}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing message from Redis: {}", message, e);
        }
    }
}
