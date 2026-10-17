package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@ActiveProfiles("local")
class LoginKeyCacheServiceTest {

    @Autowired
    private LoginKeyCacheService cacheService;

    @Autowired
    private RedisUtil redisUtil;

    @Test
    void shouldStoreEncryptedKeyWith300SecondsTTL() {
        UUID requestId = UUID.randomUUID();
        String redisKey = "auth:login-key:" + requestId;

        try {
            boolean stored = cacheService.store(
                    requestId, "encrypted-private-key-test-value");

            assertTrue(stored);

            assertEquals(
                    "encrypted-private-key-test-value",
                    redisUtil.get(redisKey)
            );

            Long ttl = redisUtil.getExpire(redisKey);

            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 300);

        } finally {
            redisUtil.delete(redisKey);
        }
    }

    @Test
    void shouldConsumeKeyOnlyOnce() {
        UUID requestId = UUID.randomUUID();
        String redisKey = "auth:login-key:" + requestId;

        try {
            cacheService.store(requestId, "encrypted-key");

            assertEquals("encrypted-key", cacheService.consume(requestId));

            assertNull(cacheService.consume(requestId));

        } finally {
            redisUtil.delete(redisKey);
        }
    }

    @Test
    void shouldRejectDuplicateRequestId() {
        UUID requestId = UUID.randomUUID();
        String redisKey = "auth:login-key:" + requestId;

        try {
            assertTrue(cacheService.store(requestId, "first-key"));

            assertFalse(cacheService.store(requestId, "second-key"));

            assertEquals("first-key", cacheService.consume(requestId));

        } finally {
            redisUtil.delete(redisKey);
        }
    }

    @Test
    void shouldReturnNullForMissingKey() {
        UUID requestId = UUID.randomUUID();

        assertNull(cacheService.consume(requestId));
    }

    @Test
    void shouldRejectInvalidInput() {
        UUID requestId = UUID.randomUUID();

        assertThrows(
                NullPointerException.class,
                () -> cacheService.store(null, "encrypted-key")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cacheService.store(requestId, "")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cacheService.store(requestId, null)
        );

        assertThrows(
                NullPointerException.class,
                () -> cacheService.consume(null)
        );
    }

    @Test
    void shouldReturnNullAfterExpiration() {
        UUID requestId = UUID.randomUUID();
        String redisKey = "auth:login-key:" + requestId;

        try {
            cacheService.store(requestId, "encrypted-key");

            // Shorten the TTL for this test only.
            // The service itself still stores keys with a 300s TTL.
            redisUtil.set(
                    redisKey,
                    "encrypted-key",
                    Duration.ofMillis(200)
            );

            // Give Redis enough time to expire the test key.
            Thread.sleep(500);

            assertNull(cacheService.consume(requestId));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Expiration test was interrupted");
        } finally {
            redisUtil.delete(redisKey);
        }
    }
}
