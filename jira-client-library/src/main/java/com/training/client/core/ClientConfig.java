package com.training.client.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Everything the client needs to reach the external system.
 * The calling service builds this once (usually from application.yml) and reuses it.
 */
@Getter
@Builder
public class ClientConfig {

    public static final String SERVICE_NAME = "JIRA_CLOUD";

    private final String url;
    private final Map<String, String> headers;
    private final String clientId;
    private final String clientSecret;
    @Builder.Default
    private final int connectionTimeout = 10;
    @Builder.Default
    private final int socketTimeout = 30;
}
