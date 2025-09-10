package org.example.userserver.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequestDto(
    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {
}
