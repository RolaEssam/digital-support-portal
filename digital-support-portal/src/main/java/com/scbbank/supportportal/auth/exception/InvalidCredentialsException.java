package com.scbbank.supportportal.auth.exception;

import java.util.Map;

public class InvalidCredentialsException extends AuthException {

    private final int remainingAttempts;

    public InvalidCredentialsException(int remainingAttempts) {
        super(ErrorCode.INVALID_CREDENTIALS);
        this.remainingAttempts = remainingAttempts;
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of("remainingAttempts", remainingAttempts);
    }
}