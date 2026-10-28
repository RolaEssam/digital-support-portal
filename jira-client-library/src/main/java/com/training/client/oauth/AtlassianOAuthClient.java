
package com.training.client.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.client.model.oauth.AtlassianTokens;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class AtlassianOAuthClient {

    private static final URI TOKEN_URI =
            URI.create("https://auth.atlassian.com/oauth/token");

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private final HttpClient httpClient;

    public AtlassianOAuthClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AtlassianTokens exchangeCode(
            String clientId,
            String clientSecret,
            String code,
            String redirectUri
    ) {
        return sendTokenRequest(Map.of(
                "grant_type", "authorization_code",
                "client_id", requireValue(clientId, "clientId"),
                "client_secret", requireValue(clientSecret, "clientSecret"),
                "code", requireValue(code, "code"),
                "redirect_uri", requireValue(redirectUri, "redirectUri")
        ));
    }

    public AtlassianTokens refresh(
            String clientId,
            String clientSecret,
            String refreshToken
    ) {
        return sendTokenRequest(Map.of(
                "grant_type", "refresh_token",
                "client_id", requireValue(clientId, "clientId"),
                "client_secret", requireValue(clientSecret, "clientSecret"),
                "refresh_token", requireValue(refreshToken, "refreshToken")
        ));
    }

    private AtlassianTokens sendTokenRequest(
            Map<String, String> requestBody
    ) {
        try {
            String json = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(TOKEN_URI)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                // Do not include response bodies or credentials
                // in exception messages or logs.
                throw new IllegalStateException(
                        "Atlassian OAuth request failed. HTTP "
                                + response.statusCode()
                );
            }

            AtlassianTokens tokens = MAPPER.readValue(
                    response.body(),
                    AtlassianTokens.class
            );

            if (tokens.getAccessToken() == null
                    || tokens.getAccessToken().isBlank()) {

                throw new IllegalStateException(
                        "Atlassian response did not contain an access token"
                );
            }

            return tokens;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Atlassian OAuth request interrupted", e
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Atlassian OAuth communication failed", e
            );
        }
    }

    private static String requireValue(
            String value,
            String name
    ) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }

        return value;
    }
}

