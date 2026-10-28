package com.training.client.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared request-building logic (headers, auth, query string).
 * A new API only overrides the small abstract methods below.
 */
public abstract class AbstractOutboundRequestBuilder<T> implements OutboundRequestGenerator<T> {

    protected abstract String getBasePath();
    protected abstract Map<String, String> getQueryParams();
    protected abstract HttpMethod getHttpMethod();

    protected Object getBody() {
        return null;
    }

    protected Map<String, Object> getAdditionalHeaders() {
        return new HashMap<>();
    }

    @Override
    public ServiceRequestPayload generateRequest(ClientConfig clientConfig) {
        return ServiceRequestPayload.builder()
                .method(getHttpMethod())
                .path(buildPath())
                .headers(buildHeaders(clientConfig))
                .body(getBody())
                .build();
    }

    private Map<String, Object> buildHeaders(ClientConfig config) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Request-ID", UUID.randomUUID().toString());

        if (isNotBlank(config.getClientId()) && isNotBlank(config.getClientSecret())) {
            String auth = config.getClientId() + ":" + config.getClientSecret();
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        }
        if (config.getHeaders() != null) {
            headers.putAll(config.getHeaders());
        }
        headers.putAll(getAdditionalHeaders());
        return headers;
    }

    protected String buildPath() {
        StringBuilder path = new StringBuilder(getBasePath());
        Map<String, String> queryParams = getQueryParams();

        if (queryParams != null && !queryParams.isEmpty()) {
            path.append("?");
            queryParams.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> {
                        if (path.charAt(path.length() - 1) != '?') {
                            path.append("&");
                        }
                        path.append(entry.getKey()).append("=").append(encodeQueryParam(entry.getValue()));
                    });
        }
        return path.toString();
    }

    protected String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected String encodePathParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
