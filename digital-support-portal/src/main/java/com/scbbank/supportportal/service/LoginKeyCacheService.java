package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.RedisUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoginKeyCacheService {

    private static final Duration KEY_TTL = Duration.ofSeconds(300);
    private static final String KEY_PREFIX = "auth:login-key:";

    private final RedisUtil redisUtil;

    public LoginKeyCacheService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    // Store an AES-encrypted RSA private key for 5 minutes.
    public boolean store(UUID requestId, String encryptedPrivateKey) {
        Objects.requireNonNull(requestId, "requestId is required");

        if (encryptedPrivateKey == null || encryptedPrivateKey.isBlank()) {
            throw new IllegalArgumentException("Encrypted private key is required");
        }

        String redisKey = KEY_PREFIX + requestId;

        // Prevent overwriting an existing unexpired key.
        return redisUtil.setIfAbsent(redisKey, encryptedPrivateKey, KEY_TTL);
    }

    // Retrieve and delete the encrypted private key atomically.
    public String consume(UUID requestId) {
        Objects.requireNonNull(requestId, "requestId is required");

        String redisKey = KEY_PREFIX + requestId;

        // Returns null if the key is missing, expired, or already consumed.
        return redisUtil.getAndDelete(redisKey);
    }
}
