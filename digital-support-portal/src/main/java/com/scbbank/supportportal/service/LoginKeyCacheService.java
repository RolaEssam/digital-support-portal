
package com.scbbank.supportportal.service;

import com.scbbank.supportportal.constants.ApplicationConstants;
import com.scbbank.supportportal.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginKeyCacheService {

    private final RedisUtil redisUtil;

    public boolean store(UUID requestId, String encryptedPrivateKey) {

        Objects.requireNonNull(
                requestId,
                "Request ID is required"
        );

        if (encryptedPrivateKey == null ||
                encryptedPrivateKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted private key is required"
            );
        }

        String redisKey =
                ApplicationConstants.LOGIN_KEY_PREFIX + requestId;

        return redisUtil.setIfAbsent(
                redisKey,
                encryptedPrivateKey,
                Duration.ofSeconds(
                        ApplicationConstants.PUBLIC_KEY_EXPIRY_SECONDS
                )
        );
    }

    public String consume(UUID requestId) {

        Objects.requireNonNull(
                requestId,
                "Request ID is required"
        );

        String redisKey =
                ApplicationConstants.LOGIN_KEY_PREFIX + requestId;

        return redisUtil.getAndDelete(redisKey);
    }
}
