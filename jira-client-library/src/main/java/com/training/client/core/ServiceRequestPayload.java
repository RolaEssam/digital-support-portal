package com.training.client.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The fully built request (method + path + headers + body) handed to the client.
 */
@Getter
@Builder
public class ServiceRequestPayload {
    private HttpMethod method;
    private String path;
    private Object body;
    private Map<String, Object> headers;
}
