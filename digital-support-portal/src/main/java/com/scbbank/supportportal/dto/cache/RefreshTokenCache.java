package com.scbbank.supportportal.dto.cache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash("refresh_token")
public class RefreshTokenCache {

    @Id
    private String tokenHash;

    @Indexed
    private Long userId;

    private String role;

    private Instant expiresAt;

    @TimeToLive
    private Long ttlSeconds;

    public RefreshTokenCache(String tokenHash, Long userId, String role,
                             Instant expiresAt, long ttlSeconds) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.role = role;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String toString() {
        return "RefreshTokenCache[userId=" + userId + ", role=" + role + "]";
    }
}
