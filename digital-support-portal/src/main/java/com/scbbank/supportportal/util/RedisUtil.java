package com.scbbank.supportportal.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key,
                java.util.concurrent.TimeUnit.SECONDS);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // Store only if the key does not already exist
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
        );
    }

    public Long incrementWithExpiry(String key, long windowSeconds) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key is required");
        }

        if (windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Rate-limit window must be positive"
            );
        }

        String luaScript = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(luaScript, Long.class);

        return redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(windowSeconds)
        );
    }

}
