package com.scbbank.supportportal.auth.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public AuthException(ErrorCode errorCode, Object... args) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = args;
    }


    @SuppressWarnings("unchecked")
    public Map<String, Object> getData() {
        if (args != null && args.length > 0 && args[0] instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }
}