package com.training.client.core.exception;

import lombok.Getter;

/**
 * 5xx from the external system: their side is broken.
 */
@Getter
public class SystemException extends CustomException {
    private final int statusCode;
    private final String responseBody;

    public SystemException(String requestId, int statusCode, String responseBody) {
        super(requestId, "System error " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
