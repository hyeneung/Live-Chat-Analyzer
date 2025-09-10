package org.example.userserver.global.exception.dto;

import lombok.Builder;

@Builder
public record CustomExceptionResponseDto(
    int status,
    String message
) {

}