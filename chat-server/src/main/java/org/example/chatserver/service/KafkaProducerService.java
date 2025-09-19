package org.example.chatserver.service;

import org.example.chatserver.dto.ChatMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for producing messages to Kafka topics.
 */
@Service
public class KafkaProducerService {

    // KafkaTemplate provides a high-level abstraction for sending messages.
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a ChatMessage to the specified Kafka topic.
     * @param topic The name of the Kafka topic.
     * @param message The message to be sent.
     */
    public void sendMessage(String topic, ChatMessage message) {
        kafkaTemplate.send(topic, UUID.randomUUID().toString(), message);
    }
}
