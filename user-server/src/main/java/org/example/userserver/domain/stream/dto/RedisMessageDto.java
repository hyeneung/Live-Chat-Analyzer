package org.example.userserver.domain.stream.dto;

public record RedisMessageDto(
        String type,
        String payload
) {
    // Static factory method
    public static RedisMessageDto from(String type, String payload) {
        return new RedisMessageDto(type, payload);
    }
}
