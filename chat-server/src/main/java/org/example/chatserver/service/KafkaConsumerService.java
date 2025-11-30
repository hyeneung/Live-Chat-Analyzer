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
        redisPublisherService.publish(message.streamId(), "analysis", message);
    }

    @KafkaListener(topics = "${kafka.topic.summary-results}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "summaryResultListenerContainerFactory")
    public void listenSummaryResult(SummaryResultDto message) {
        String redisKey = "summary:" + message.streamId();
        redisTemplate.opsForValue().set(redisKey, message.summary());

        // Publish summary to the Redis backplane for broadcasting to WebSocket clients
        redisPublisherService.publish(message.streamId(), "summary", message);
        logger.info("Published summary to Redis backplane for broadcasting");
    }
}
