package org.example.chatserver.controller;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.example.chatserver.dto.ChatMessage;
import org.example.chatserver.service.KafkaProducerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling chat messages via WebSocket.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    @Value("${kafka.topic.raw-chats}")
    private String rawChatsTopic;

    private final KafkaProducerService producerService;

    /**
     * Handles incoming chat messages from clients.
     * Messages sent to the "/publish/{roomId}" destination are routed to this method.
     * @param chatMessage The chat message payload.
     */
    @MessageMapping("/{roomId}")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // publish to kafka
        producerService.sendMessage(rawChatsTopic, chatMessage);
    }
}
