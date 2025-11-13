package org.example.chatserver.service;

import org.example.chatserver.dto.AnalysisResultDto;
import org.example.chatserver.dto.ChatMessageDto;
import org.example.chatserver.dto.SummaryResultDto;
import org.example.chatserver.service.RedisPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka topics.
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final RedisPublisherService redisPublisherService;
    private final RedisTemplate<String, Object> redisTemplate;

    public KafkaConsumerService(RedisPublisherService redisPublisherService, RedisTemplate<String, Object> redisTemplate) {
        this.redisPublisherService = redisPublisherService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Listens for messages on the "analysis-result" Kafka topic.
     * Once a message is received, it is published to the Redis backplane for broadcasting.
     * @param message The chat message received from Kafka, which includes analysis results.
     */
    @KafkaListener(topics = "${kafka.topic.analysis-result}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "analysisResultListenerContainerFactory")
    public void listenAnalysisResult(AnalysisResultDto message) {
        redisPublisherService.publish("analysis", message);
    }

    /**
     * Listens for messages on the "raw-chats" Kafka topic.
     * Once a message is received, it is published to the Redis backplane for broadcasting.
     * @param message The raw chat message received from Kafka.
     */
    @KafkaListener(topics = "${kafka.topic.raw-chats}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenRawChats(ChatMessageDto message) {
        redisPublisherService.publish("chat", message);
    }

    @KafkaListener(topics = "${kafka.topic.summary-results}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "summaryResultListenerContainerFactory")
    public void listenSummaryResult(SummaryResultDto message) {
        logger.info("Received summary result: {}", message);
        String redisKey = "summary:" + message.streamId();
        logger.info("Saving summary to Redis with key: {}", redisKey);
        redisTemplate.opsForValue().set(redisKey, message.summary());

        // Publish summary to the Redis backplane for broadcasting to WebSocket clients
        redisPublisherService.publish("summary", message);
        logger.info("Published summary to Redis backplane for broadcasting");
    }
}
