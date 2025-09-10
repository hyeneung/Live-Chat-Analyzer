package org.example.userserver.domain.user.dto.response;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record GoogleLoginResponseDto(
    @NonNull Long userId,
    @NonNull String userRole,
    String profileImage,
    @NonNull String accessToken,
    @NonNull String refreshToken
) {
}
