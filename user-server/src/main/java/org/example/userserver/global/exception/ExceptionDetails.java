package org.example.userserver.global.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionDetails {

    HttpStatus getStatus();

    String getMessage();
}