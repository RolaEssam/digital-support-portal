package com.training.client.core.exception;

import lombok.Getter;

/**
 * 4xx from the external system: the request was wrong (not found, invalid input...).
 */
@Getter
public class BusinessException extends CustomException {
    private final int statusCode;
    private final String responseBody;

    public BusinessException(String requestId, int statusCode, String responseBody) {
        super(requestId, "Business error " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
