package org.example.chatserver.controller;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatserver.dto.ChatMessageDto;
import org.example.chatserver.service.KafkaProducerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling chat messages via WebSocket.
 */
@Slf4j // Add this annotation for logging
@Controller
@RequiredArgsConstructor
public class ChatController {

    @Value("${kafka.topic.raw-chats}")
    private String rawChatsTopic;

    private final KafkaProducerService producerService;

    /**
     * Handles incoming chat messages from clients.
     * Messages sent to the "/publish/{roomId}" destination are routed to this method.
     * @param chatMessageDto The chat message payload.
     */
    @MessageMapping("/{roomId}")
    public void sendMessage(@Payload ChatMessageDto chatMessageDto) {
        log.info("Received chat message from client: {}", chatMessageDto); // Log the incoming message
        // publish to kafka
        producerService.sendMessage(rawChatsTopic, chatMessageDto);
    }
}
