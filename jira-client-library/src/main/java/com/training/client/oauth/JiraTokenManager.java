
package com.training.client.oauth;

import com.training.client.model.oauth.AtlassianTokens;

import java.time.Instant;
import java.util.Objects;

public class JiraTokenManager {

    private static final long EXPIRY_BUFFER_SECONDS = 60;

    private final AtlassianOAuthClient oauthClient;
    private final JiraTokenStore tokenStore;
    private final String clientId;
    private final String clientSecret;

    public JiraTokenManager(
            AtlassianOAuthClient oauthClient,
            JiraTokenStore tokenStore,
            String clientId,
            String clientSecret
    ) {
        this.oauthClient = Objects.requireNonNull(oauthClient);
        this.tokenStore = Objects.requireNonNull(tokenStore);
        this.clientId = requireValue(clientId, "clientId");
        this.clientSecret = requireValue(clientSecret, "clientSecret");
    }

    public synchronized String getValidAccessToken() {

        StoredJiraTokens stored = Objects.requireNonNull(
                tokenStore.load(),
                "No Jira connection found"
        );

        if (Instant.now().isBefore(stored.expiresAt())) {
            return stored.accessToken();
        }

        String refreshToken = requireValue(
                stored.refreshToken(),
                "refreshToken"
        );

        AtlassianTokens refreshed = oauthClient.refresh(
                clientId,
                clientSecret,
                refreshToken
        );

        String newAccessToken = requireValue(
                refreshed.getAccessToken(),
                "accessToken"
        );

        String newRefreshToken = requireValue(
                refreshed.getRefreshToken(),
                "refreshToken"
        );

        Long expiresIn = Objects.requireNonNull(
                refreshed.getExpiresIn(),
                "Token expiry is required"
        );

        if (expiresIn <= 0) {
            throw new IllegalStateException(
                    "Invalid access-token expiry"
            );
        }

        StoredJiraTokens updated = new StoredJiraTokens(
                newAccessToken,
                newRefreshToken,
                Instant.now().plusSeconds(
                        Math.max(0, expiresIn - EXPIRY_BUFFER_SECONDS)
                )
        );

        // Save the new rotating refresh token BEFORE returning
        // the access token to the caller.
        tokenStore.save(updated);

        return updated.accessToken();
    }

    private static String requireValue(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
