package com.example.urlshortener.scheduler;

import com.example.urlshortener.entity.Url;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.service.CacheService;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExpirationSchedulerIntegrationTest extends IntegrationTestBase {

    @Autowired UrlService urlService;
    @Autowired UrlRepository urlRepository;
    @Autowired CacheService cacheService;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    void shouldDeactivateExpiredUrlAndEvictCache() {
        // Criar URL expirada
        Url expired = new Url("https://expired.example.com", "exp0001", 
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60));
        urlRepository.save(expired);
        
        // Adicionar no cache
        cacheService.putRedirection("exp0001", expired.getOriginalUrl(), Duration.ofMinutes(5));

        // Verificar que está no cache
        assertThat(redisTemplate.opsForValue().get("redirect:exp0001")).isNotNull();

        // Executar depreciação de URLs expiradas
        int count = urlService.deactivateExpired();

        // Verificar resultados
        assertThat(count).isGreaterThanOrEqualTo(1);
        assertThat(redisTemplate.opsForValue().get("redirect:exp0001")).isNull();

        Url reloaded = urlRepository.findByShortCode("exp0001").orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
    }
}