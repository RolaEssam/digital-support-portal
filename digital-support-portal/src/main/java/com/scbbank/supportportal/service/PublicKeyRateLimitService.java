package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PublicKeyRateLimitService {

    private static final String KEY_PREFIX =
            "auth:public-key:rate-limit:";

    private final RedisUtil redisUtil;
    private final long maxRequests;
    private final long windowSeconds;

    public PublicKeyRateLimitService(
            RedisUtil redisUtil,
            @Value("${auth.rate-limit.public-key.max-requests:20}")
            long maxRequests,
            @Value("${auth.rate-limit.public-key.window-seconds:60}")
            long windowSeconds) {

        if (maxRequests <= 0 || windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Invalid public-key rate-limit configuration"
            );
        }

        this.redisUtil = redisUtil;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean isAllowed(String clientIdentifier) {

        if (clientIdentifier == null ||
                clientIdentifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Client identifier is required"
            );
        }

        // TODO(AHMED): Define and supply a trusted client identifier.

        String redisKey = KEY_PREFIX + clientIdentifier;

        Long count = redisUtil.incrementWithExpiry(
                redisKey,
                windowSeconds
        );

        if (count == null) {
            throw new IllegalStateException(
                    "Unable to evaluate public-key rate limit"
            );
        }

        // TODO(AHMED): Map a rejected request to HTTP 429
        // with error code DSP-AUTH-008 at the API boundary.

        return count <= maxRequests;
    }
}
