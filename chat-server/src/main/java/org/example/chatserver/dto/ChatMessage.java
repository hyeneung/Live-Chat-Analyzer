package org.example.chatserver.dto;

/**
 * Data Transfer Object for a chat message, using nested records for a concise and immutable structure.
 *
 * @param sender   Information about the message sender.
 * @param content  The text content of the message.
 * @param streamId The ID of the stream or chat room.
 */
public record ChatMessage(
        SenderInfo sender,
        String content,
        String streamId
) {
    /**
     * A nested record to hold information about the sender.
     *
     * @param id              The unique identifier of the sender.
     * @param name            The display name of the sender.
     * @param profileImageUrl The URL for the sender's profile image.
     */
    public record SenderInfo(
            String id,
            String name,
            String profileImageUrl
    ) {}
}
