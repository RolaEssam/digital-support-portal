package com.scbbank.supportportal.dto.cache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Duration;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash("login_attempt")
public class LoginAttemptCache {

    @Id
    private String username;

    private int failedCount;

    private Instant lockedUntil;      // null = not locked

    @TimeToLive
    private long ttlSeconds;

    public LoginAttemptCache(String username) {
        this.username = username;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailure(Instant now, int maxAttempts, Duration lockDuration, Duration window) {
        failedCount++;
        if (failedCount >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            ttlSeconds = lockDuration.toSeconds();
        } else {
            ttlSeconds = window.toSeconds();
        }
    }
}