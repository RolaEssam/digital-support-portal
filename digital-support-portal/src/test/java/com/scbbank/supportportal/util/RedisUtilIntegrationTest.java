
package com.scbbank.supportportal.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class RedisUtilIntegrationTest {

    @Autowired
    private RedisUtil redisUtil;

    @Test
    void shouldStoreAndRetrieveValue() {

        String key = "test:redis:" + UUID.randomUUID();

        try {
            redisUtil.set(key, "Hello Redis", Duration.ofSeconds(300));

            assertEquals("Hello Redis", redisUtil.get(key));
            assertTrue(redisUtil.exists(key));

            Long ttl = redisUtil.getExpire(key);
            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 300);

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldConsumeValueOnlyOnce() {

        String key = "test:redis:" + UUID.randomUUID();

        try {
            redisUtil.set(key, "private-key", Duration.ofSeconds(300));

            assertEquals("private-key", redisUtil.getAndDelete(key));

            // The same key cannot be consumed again
            assertNull(redisUtil.getAndDelete(key));
            assertFalse(redisUtil.exists(key));

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldIncrementCounter() {

        String key = "test:counter:" + UUID.randomUUID();

        try {
            assertEquals(1L, redisUtil.increment(key));
            assertEquals(2L, redisUtil.increment(key));

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldReserveUniqueKey() {

        String key = "test:request:" + UUID.randomUUID();

        try {
            assertTrue(redisUtil.setIfAbsent(
                    key, "reserved", Duration.ofSeconds(300)));

            // Duplicate reservation must fail
            assertFalse(redisUtil.setIfAbsent(
                    key, "reserved", Duration.ofSeconds(300)));

        } finally {
            redisUtil.delete(key);
        }
    }

    @Test
    void shouldDeleteValue() {

        String key = "test:delete:" + UUID.randomUUID();

        try {
            redisUtil.set(key, "temporary", Duration.ofSeconds(300));

            assertTrue(redisUtil.delete(key));
            assertNull(redisUtil.get(key));
            assertFalse(redisUtil.exists(key));

        } finally {
            redisUtil.delete(key);
        }
    }
}
