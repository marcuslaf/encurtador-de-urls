package com.example.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void putRedirection_shouldStoreWithTtl() {
        String shortCode = "abc1234";
        String originalUrl = "https://example.com";
        Duration ttl = Duration.ofHours(1);

        cacheService.putRedirection(shortCode, originalUrl, ttl);

        verify(valueOperations).set("redirect:abc1234", originalUrl, ttl);
    }

    @Test
    void getRedirection_shouldReturnUrlWhenExists() {
        String shortCode = "abc1234";
        when(valueOperations.get("redirect:abc1234")).thenReturn("https://example.com");

        Optional<String> result = cacheService.getRedirection(shortCode);

        assertThat(result).contains("https://example.com");
    }

    @Test
    void getRedirection_shouldReturnEmptyWhenNotExists() {
        String shortCode = "abc1234";
        when(valueOperations.get("redirect:abc1234")).thenReturn(null);

        Optional<String> result = cacheService.getRedirection(shortCode);

        assertThat(result).isEmpty();
    }

    @Test
    void evictRedirection_shouldDeleteKey() {
        String shortCode = "abc1234";

        cacheService.evictRedirection(shortCode);

        verify(redisTemplate).delete("redirect:abc1234");
    }
}
