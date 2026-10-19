package com.scbbank.supportportal.dto.cache;

import com.scbbank.supportportal.dto.cache.LoginAttemptCache;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptCacheTest {

    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private final Instant now = Instant.parse("2026-10-13T12:00:00Z");

    @Test
    void newEntry_isNotLocked() {
        LoginAttemptCache a = new LoginAttemptCache("mona");
        assertEquals(0, a.getFailedCount());
        assertNull(a.getLockedUntil());
        assertFalse(a.isLocked(now));
    }

    @Test
    void failuresBelowLimit_doNotLock_andUseWindowTtl() {
        LoginAttemptCache a = new LoginAttemptCache("mona");
        for (int i = 0; i < 4; i++) a.recordFailure(now, 5, LOCK, WINDOW);

        assertEquals(4, a.getFailedCount());
        assertFalse(a.isLocked(now));
        assertEquals(600, a.getTtlSeconds());
    }

    @Test
    void fifthFailure_locksForLockDuration() {
        LoginAttemptCache a = new LoginAttemptCache("mona");
        for (int i = 0; i < 5; i++) a.recordFailure(now, 5, LOCK, WINDOW);

        assertTrue(a.isLocked(now));
        assertEquals(now.plus(LOCK), a.getLockedUntil());
        assertEquals(900, a.getTtlSeconds());
    }

    @Test
    void lock_expiresAfterLockedUntil() {
        LoginAttemptCache a = new LoginAttemptCache("mona");
        for (int i = 0; i < 5; i++) a.recordFailure(now, 5, LOCK, WINDOW);

        assertTrue(a.isLocked(now.plus(LOCK).minusSeconds(1)));
        assertFalse(a.isLocked(now.plus(LOCK)));
    }
}