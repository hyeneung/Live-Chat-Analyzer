package org.example.userserver.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record TokenReissueResponseDto(
    @NotBlank String accessToken,
    @NotBlank String refreshToken
) {
}
