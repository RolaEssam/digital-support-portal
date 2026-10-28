
package com.training.client.oauth;

import java.time.Instant;

public record StoredJiraTokens(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}
