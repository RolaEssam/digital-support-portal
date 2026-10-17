package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@ActiveProfiles("local")
class PublicKeyRateLimitServiceTest {

    @Autowired
    private PublicKeyRateLimitService rateLimitService;

    @Autowired
    private RedisUtil redisUtil;

    private static final String URI = "/api/v1/auth/public-key";
    private static final String PREFIX = "auth:rl:";

    private String newClientIp() {
        return "test-client-" + UUID.randomUUID();
    }

    private String redisKey(String ip, String uri) {
        return PREFIX + ip + ":" + uri;
    }

    @Test
    void shouldAllowTenRequestsAndRejectEleventh() {
        String ip = newClientIp();

        try {
            for (int i = 1; i <= 10; i++) {
                assertTrue(
                        rateLimitService.isAllowed(ip, URI),
                        "Request " + i + " should be allowed"
                );
            }

            assertFalse(rateLimitService.isAllowed(ip, URI));

        } finally {
            redisUtil.delete(redisKey(ip, URI));
        }
    }

    @Test
    void shouldMaintainSeparateCountersForDifferentIps() {
        String ipA = newClientIp();
        String ipB = newClientIp();

        try {
            for (int i = 0; i < 10; i++) {
                assertTrue(rateLimitService.isAllowed(ipA, URI));
            }

            assertFalse(rateLimitService.isAllowed(ipA, URI));
            assertTrue(rateLimitService.isAllowed(ipB, URI));

        } finally {
            redisUtil.delete(redisKey(ipA, URI));
            redisUtil.delete(redisKey(ipB, URI));
        }
    }

    @Test
    void shouldMaintainSeparateCountersForDifferentUris() {
        String ip = newClientIp();
        String loginUri = "/api/v1/auth/login";

        try {
            for (int i = 0; i < 10; i++) {
                assertTrue(rateLimitService.isAllowed(ip, URI));
            }

            assertFalse(rateLimitService.isAllowed(ip, URI));
            assertTrue(rateLimitService.isAllowed(ip, loginUri));

        } finally {
            redisUtil.delete(redisKey(ip, URI));
            redisUtil.delete(redisKey(ip, loginUri));
        }
    }

    @Test
    void shouldSetSixtySecondExpiry() {
        String ip = newClientIp();

        try {
            assertTrue(rateLimitService.isAllowed(ip, URI));

            // Redis getExpire returns whole seconds.
            Long ttl = redisUtil.getExpire(redisKey(ip, URI));

            assertNotNull(ttl);
            assertTrue(ttl >= 0 && ttl <= 60);

        } finally {
            redisUtil.delete(redisKey(ip, URI));
        }
    }

    @Test
    void shouldResetCounterAfterExpiry() throws Exception {
        String ip = newClientIp();
        String key = redisKey(ip, URI);

        try {
            for (int i = 0; i < 10; i++) {
                assertTrue(rateLimitService.isAllowed(ip, URI));
            }

            assertFalse(rateLimitService.isAllowed(ip, URI));

            // Shorten the existing TTL to simulate expiration.
            redisUtil.set(key, "11", Duration.ofMillis(200));

            Thread.sleep(500);

            assertTrue(rateLimitService.isAllowed(ip, URI));

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldRejectMissingIpOrUri() {
        String ip = newClientIp();

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed(null, URI)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed("", URI)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed(ip, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.isAllowed(ip, "")
        );
    }

    @Test
    void shouldFailClosedWhenRedisReturnsNull() {
        RedisUtil failingRedis = mock(RedisUtil.class);

        when(failingRedis.incrementWithExpiry(
                anyString(), anyLong()
        )).thenReturn(null);

        PublicKeyRateLimitService service =
                new PublicKeyRateLimitService(failingRedis);

        assertThrows(
                IllegalStateException.class,
                () -> service.isAllowed("192.0.2.10", URI)
        );
    }

    @Test
    void shouldPropagateRedisConnectionFailures() {
        RedisUtil failingRedis = mock(RedisUtil.class);

        when(failingRedis.incrementWithExpiry(
                anyString(), anyLong()
        )).thenThrow(
                new IllegalStateException("Redis unavailable")
        );

        PublicKeyRateLimitService service =
                new PublicKeyRateLimitService(failingRedis);

        assertThrows(
                IllegalStateException.class,
                () -> service.isAllowed("192.0.2.10", URI)
        );
    }
}
