package com.scbbank.supportportal.dto.cache;

import lombok.AllArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refresh_token")
public class RefreshTokenCache {

    @Id
    private String tokenHash;

    @Indexed
    private Long userId;

    private Instant expiresAt;

    @TimeToLive
    private Long ttlSeconds;
}
