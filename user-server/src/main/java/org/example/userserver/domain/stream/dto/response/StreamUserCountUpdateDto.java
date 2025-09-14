package org.example.userserver.domain.stream.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record StreamUserCountUpdateDto(
        @NotBlank String streamId,
        @NonNull Long userCount
) {
}