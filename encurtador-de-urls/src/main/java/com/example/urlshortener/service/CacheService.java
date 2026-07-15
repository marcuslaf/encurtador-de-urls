package com.example.urlshortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    public static final String REDIRECTION_KEY_PREFIX = "redirect:";

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public CacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putRedirection(String shortCode, String originalUrl, Duration ttl) {
        redisTemplate.opsForValue().set(REDIRECTION_KEY_PREFIX + shortCode, originalUrl, ttl);
    }

    public Optional<String> getRedirection(String shortCode) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REDIRECTION_KEY_PREFIX + shortCode));
    }

    public void evictRedirection(String shortCode) {
        redisTemplate.delete(REDIRECTION_KEY_PREFIX + shortCode);
    }
}
