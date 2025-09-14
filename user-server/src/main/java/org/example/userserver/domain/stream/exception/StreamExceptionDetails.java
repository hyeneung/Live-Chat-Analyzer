package org.example.userserver.domain.stream.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.userserver.global.exception.ExceptionDetails;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StreamExceptionDetails implements ExceptionDetails {
    STREAM_NOT_FOUND(HttpStatus.NOT_FOUND, "Stream not found");

    private final HttpStatus status;
    private final String message;
}
