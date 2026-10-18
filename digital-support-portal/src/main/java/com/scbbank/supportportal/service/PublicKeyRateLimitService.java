package com.scbbank.supportportal.service;

import com.scbbank.supportportal.constants.ApplicationConstants;
import com.scbbank.supportportal.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicKeyRateLimitService {



    private final RedisUtil redisUtil;

    public boolean isAllowed(String clientIp, String requestUri) {

        if (clientIp == null || clientIp.isBlank()
                || requestUri == null || requestUri.isBlank()) {

            throw new IllegalArgumentException(
                    "Client IP and request URI are required"
            );
        }

        String redisKey =
                ApplicationConstants.RATE_LIMIT_KEY_PREFIX
                        + clientIp + ":" + requestUri;

        Long count = redisUtil.incrementWithExpiry(
                redisKey,
                ApplicationConstants.RATE_LIMIT_WINDOW_SECONDS
        );

        if (count == null) {
            throw new IllegalStateException(
                    "Unable to evaluate API rate limit"
            );
        }

        return count <= ApplicationConstants.RATE_LIMIT_MAX_REQUESTS;
    }
}
