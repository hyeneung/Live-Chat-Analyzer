package org.example.userserver.domain.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.userserver.global.exception.ExceptionDetails;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserExceptionDetails implements ExceptionDetails {

    // Token related exceptions
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token"),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "Refresh Token mismatch or not found in Redis"),

    // User related exceptions
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found");

    private final HttpStatus status;
    private final String message;
}