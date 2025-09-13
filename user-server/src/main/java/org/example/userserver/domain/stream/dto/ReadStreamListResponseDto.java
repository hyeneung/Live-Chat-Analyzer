package org.example.userserver.domain.stream.dto;

import lombok.Builder;
import lombok.NonNull;
import org.example.userserver.domain.stream.entity.Stream;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record ReadStreamListResponseDto(
    @NonNull List<StreamDto> streams,
    boolean hasNext,
    int numberOfElements,
    int pageNumber,
    int pageSize
) {

    public static ReadStreamListResponseDto from(Slice<Stream> streamSlice) {
        List<StreamDto> streamDtos = streamSlice.getContent()
            .stream()
            .map(StreamDto::from)
            .collect(Collectors.toList());

        return ReadStreamListResponseDto.builder()
            .streams(streamDtos)
            .hasNext(streamSlice.hasNext())
            .numberOfElements(streamSlice.getNumberOfElements())
            .pageNumber(streamSlice.getNumber())
            .pageSize(streamSlice.getSize())
            .build();
    }

    @Builder
    public record StreamDto(
        @NonNull Long id,
        @NonNull Long hostId,
        @NonNull String title,
        String thumbnailUrl,
        @NonNull String hostname,
        String hostprofile,
        @NonNull LocalDateTime createdAt
    ) {
        public static StreamDto from(Stream stream) {
            return StreamDto.builder()
                .id(stream.getId())
                .hostId(stream.getHost().getId())
                .title(stream.getTitle())
                .thumbnailUrl(stream.getThumbnailUrl())
                .hostname(stream.getHost().getName())
                .hostprofile(stream.getHost().getProfileImage())
                .createdAt(stream.getCreatedAt())
                .build();
        }
    }
}
