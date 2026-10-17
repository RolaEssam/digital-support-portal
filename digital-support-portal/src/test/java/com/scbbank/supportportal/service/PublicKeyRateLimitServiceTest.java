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
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
        "auth.rate-limit.public-key.max-requests=20",
        "auth.rate-limit.public-key.window-seconds=60"
})
@ActiveProfiles("local")
class PublicKeyRateLimitServiceTest {

    @Autowired
    private PublicKeyRateLimitService rateLimitService;

    @Autowired
    private RedisUtil redisUtil;

    private String redisKey(String clientId) {
        return "auth:public-key:rate-limit:" + clientId;
    }

    @Test
    void shouldAllowFirstTwentyRequestsAndRejectTwentyFirst() {

        String clientId = UUID.randomUUID().toString();

        try {
            for (int i = 1; i <= 20; i++) {
                assertTrue(
                        rateLimitService.isAllowed(clientId),
                        "Request " + i + " should be allowed"
                );
            }

            assertFalse(
                    rateLimitService.isAllowed(clientId),
                    "Request 21 should be rejected"
            );

        } finally {
            redisUtil.delete(redisKey(clientId));
        }
    }

    @Test
    void shouldMaintainSeparateCountersForDifferentClients() {

        String clientA = UUID.randomUUID().toString();
        String clientB = UUID.randomUUID().toString();

        try {
            for (int i = 0; i < 20; i++) {
                assertTrue(rateLimitService.isAllowed(clientA));
            }

            assertFalse(rateLimitService.isAllowed(clientA));
            assertTrue(rateLimitService.isAllowed(clientB));

        } finally {
            redisUtil.delete(redisKey(clientA));
            redisUtil.delete(redisKey(clientB));
        }
    }

    @Test
    void shouldAssignExpiryToRateLimitKey() {

        String clientId = UUID.randomUUID().toString();

        try {
            assertTrue(rateLimitService.isAllowed(clientId));

            Long ttl = redisUtil.getExpire(redisKey(clientId));

            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 60);

        } finally {
            redisUtil.delete(redisKey(clientId));
        }
    }

    @Test
    void shouldAllowRequestsAfterWindowExpires() throws Exception {

        String clientId = UUID.randomUUID().toString();
        String key = redisKey(clientId);

        try {
            for (int i = 0; i < 20; i++) {
                assertTrue(rateLimitService.isAllowed(clientId));
            }

            assertFalse(rateLimitService.isAllowed(clientId));

            // Simulate expiry without waiting 60 seconds.
            redisUtil.set(key, "21", Duration.ofMillis(200));

            Thread.sleep(500);

            assertTrue(rateLimitService.isAllowed(clientId));

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldRejectMissingClientIdentifier() {

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed("")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed("   ")
        );
    }

    @Test
    void shouldNotAllowRequestsWhenRedisEvaluationFails() {

        RedisUtil failingRedisUtil =
                org.mockito.Mockito.mock(RedisUtil.class);

        org.mockito.Mockito.when(
                failingRedisUtil.incrementWithExpiry(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong()
                )
        ).thenReturn(null);

        PublicKeyRateLimitService service =
                new PublicKeyRateLimitService(
                        failingRedisUtil, 20, 60
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.isAllowed("test-client")
        );
    }

}
