package org.example.chatserver.service;

import org.example.chatserver.dto.ChatMessageDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for producing messages to Kafka topics.
 */
@Service
public class KafkaProducerService {

    // KafkaTemplate provides a high-level abstraction for sending messages.
    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;
    private final RedisPublisherService redisPublisherService;

    public KafkaProducerService(KafkaTemplate<String, ChatMessageDto> kafkaTemplate, RedisPublisherService redisPublisherService) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisPublisherService = redisPublisherService;
    }

    /**
     * Sends a ChatMessage to the specified Kafka topic.
     * @param topic The name of the Kafka topic.
     * @param message The message to be sent.
     */
    public void sendMessage(String topic, ChatMessageDto message) {
        kafkaTemplate.send(topic, UUID.randomUUID().toString(), message);
        redisPublisherService.publish(message.streamId(), "chat", message);
    }
}
