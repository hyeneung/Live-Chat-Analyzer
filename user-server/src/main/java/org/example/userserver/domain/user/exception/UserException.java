package org.example.userserver.domain.user.exception;

import org.example.userserver.global.exception.CustomException;
import org.example.userserver.global.exception.ExceptionDetails;

public class UserException extends CustomException {
    public UserException(ExceptionDetails exceptionDetails) {
        super(exceptionDetails);
    }
}