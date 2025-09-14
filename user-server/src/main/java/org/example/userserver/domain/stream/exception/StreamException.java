package org.example.userserver.domain.stream.exception;

import org.example.userserver.global.exception.CustomException;
import org.example.userserver.global.exception.ExceptionDetails;

public class StreamException extends CustomException {
    public StreamException(ExceptionDetails exceptionDetails) {
        super(exceptionDetails);
    }
}