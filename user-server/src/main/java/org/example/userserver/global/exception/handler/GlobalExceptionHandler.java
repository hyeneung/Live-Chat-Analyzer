package org.example.userserver.global.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.userserver.global.exception.CustomException;
import org.example.userserver.global.exception.dto.CustomExceptionResponseDto;
import org.example.userserver.global.exception.ExceptionDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomExceptionResponseDto> customExceptionHandler(CustomException exception) {
        ExceptionDetails exceptionDetails = exception.getExceptionDetails();
        log.error(
            "Custom exception occurred {} : {}",
            exception.getClass().getSimpleName(),
            exceptionDetails.getMessage()
        );
        return ResponseEntity
            .status(exceptionDetails.getStatus())
            .body(CustomExceptionResponseDto.builder()
                .status(exceptionDetails.getStatus().value())
                .message(exceptionDetails.getMessage())
                .build()
            );
    }
}