package org.example.chatserver.service;

import org.example.chatserver.config.WebSocketConstants;
import org.example.chatserver.dto.AnalysisResultDto;
import org.example.chatserver.dto.ChatMessageDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka topics.
 */
@Service
public class KafkaConsumerService {

    // Template for sending messages to WebSocket clients.
    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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
}
