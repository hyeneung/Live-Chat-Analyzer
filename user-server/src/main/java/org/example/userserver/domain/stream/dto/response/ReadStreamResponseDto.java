package org.example.userserver.domain.stream.dto.response;

import lombok.Builder;
import lombok.NonNull;
import org.example.userserver.domain.stream.entity.Stream;

@Builder
public record ReadStreamResponseDto(
        @NonNull Long id,
        @NonNull String title,
        @NonNull HostDto host,
        long viewerCount,
        String summary
) {
    @Builder
    public record HostDto(
            @NonNull String name,
            String profilePic
    ) {}

    public static ReadStreamResponseDto from(Stream stream, long viewerCount, String summary) {
        return ReadStreamResponseDto.builder()
                .id(stream.getId())
                .title(stream.getTitle())
                .host(HostDto.builder()
                        .name(stream.getHost().getName())
                        .profilePic(stream.getHost().getProfileImage())
                        .build())
                .viewerCount(viewerCount)
                .summary(summary)
                .build();
    }
}
