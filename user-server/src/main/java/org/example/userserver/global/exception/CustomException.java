package org.example.userserver.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final ExceptionDetails exceptionDetails;

    public CustomException(ExceptionDetails exceptionDetails) {
        super(exceptionDetails.getMessage());
        this.exceptionDetails = exceptionDetails;
    }
}