package com.scbbank.supportportal.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("login_attempt")
public class LoginAttemptCache {

    @Id
    private String username;

    private int failedCount;
    private Instant lockedUntil;

    @TimeToLive
    private Long ttlSeconds; // time until redies delete
}
