package com.training.client.core.exception;

import lombok.Getter;

/**
 * Base exception for every failure coming out of the library.
 */
@Getter
public class CustomException extends RuntimeException {
    private final String requestId;

    public CustomException(String requestId, String message) {
        super(message);
        this.requestId = requestId;
    }
}
