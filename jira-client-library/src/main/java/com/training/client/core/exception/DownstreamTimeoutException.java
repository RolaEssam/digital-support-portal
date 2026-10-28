package com.training.client.core.exception;

import lombok.Getter;

/**
 * The external system could not be reached or did not answer in time.
 */
@Getter
public class DownstreamTimeoutException extends CustomException {
    private final String timeoutType;

    public DownstreamTimeoutException(String requestId, String timeoutType, String message) {
        super(requestId, message);
        this.timeoutType = timeoutType;
    }
}
