package com.scbbank.supportportal.auth.exception;

import java.util.Map;

public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public AuthException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Object[] getArgs()       { return args; }

    public Map<String, Object> getDetails() {
        return Map.of();
    }
}