package org.example.chatserver.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record StreamUserCountUpdateDto(
        @NonNull String streamId,
        @NonNull Long userCount
) {
}
