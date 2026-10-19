package com.scbbank.supportportal.auth.exception;

import java.util.Map;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException(int remainingAttempts) {
        super(ErrorCode.INVALID_CREDENTIALS, Map.of("remainingAttempts", remainingAttempts));
    }
}