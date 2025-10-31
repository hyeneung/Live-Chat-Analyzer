package org.example.chatserver.service;

import org.example.chatserver.config.WebSocketConstants;
import org.example.chatserver.dto.AnalysisResultDto;
import org.example.chatserver.dto.ChatMessageDto;
import org.example.chatserver.dto.SummaryResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka topics.
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    // Template for sending messages to WebSocket clients.
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Listens for messages on the "analysis-result" Kafka topic.
     * Once a message is received, it is forwarded to the appropriate WebSocket topic based on the streamId.
     * @param message The chat message received from Kafka, which includes analysis results.
     */
    @KafkaListener(topics = "${kafka.topic.analysis-result}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "analysisResultListenerContainerFactory")
    public void listenAnalysisResult(AnalysisResultDto message) {
        // Construct the WebSocket topic destination dynamically using the streamId.
        String destination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + message.streamId() + "/analysis";
        // Send the message to all subscribers of the destination.
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Listens for messages on the "raw-chats" Kafka topic.
     * Once a message is received, it is forwarded to the appropriate WebSocket topic based on the streamId.
     * This ensures that all chat messages are broadcasted to clients after being processed by Kafka.
     * @param message The raw chat message received from Kafka.
     */
    @KafkaListener(topics = "${kafka.topic.raw-chats}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenRawChats(ChatMessageDto message) {
        String destination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + message.streamId() + "/message";
        messagingTemplate.convertAndSend(destination, message);
    }

    @KafkaListener(topics = "${kafka.topic.summary-results}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "summaryResultListenerContainerFactory")
    public void listenSummaryResult(SummaryResultDto message) {
        logger.info("Received summary result: {}", message);
        String redisKey = "summary:" + message.streamId();
        logger.info("Saving summary to Redis with key: {}", redisKey);
        redisTemplate.opsForValue().set(redisKey, message.summary());

        // Send summary to WebSocket clients
        String destination = WebSocketConstants.TOPIC_PREFIX + "/stream/" + message.streamId() + "/summary";
        messagingTemplate.convertAndSend(destination, message);
        logger.info("Sent summary to WebSocket destination: {}", destination);
    }
}
