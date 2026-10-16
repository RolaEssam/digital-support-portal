
package com.scbbank.supportportal.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Store a value with an expiration time
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    // Retrieve a value without deleting it
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Retrieve and delete atomically (one-time use)
    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    // Delete a key
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // Check whether a key exists
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Get the remaining expiration time
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key,
                java.util.concurrent.TimeUnit.SECONDS);
    }

    // Increment a numeric value atomically
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // Store only if the key does not already exist
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
        );
    }
}
