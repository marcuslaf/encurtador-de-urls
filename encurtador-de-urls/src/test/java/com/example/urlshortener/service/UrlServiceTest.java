package com.example.urlshortener.service;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.CreateUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.AccessLog;
import com.example.urlshortener.entity.Url;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.AccessLogRepository;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UrlServiceTest {

    private UrlRepository urlRepository;
    private AccessLogRepository accessLogRepository;
    private ShortCodeGenerator shortCodeGenerator;
    private CacheService cacheService;
    private UrlService urlService;

    private static final String CODE = "abc1234";
    private static final String URL_STR = "https://example.com";

    @BeforeEach
    void setUp() {
        urlRepository = mock(UrlRepository.class);
        accessLogRepository = mock(AccessLogRepository.class);
        shortCodeGenerator = mock(ShortCodeGenerator.class);
        cacheService = mock(CacheService.class);
        urlService = new UrlService(urlRepository, accessLogRepository, shortCodeGenerator, cacheService,
                "http://localhost:8080", 1440);
    }

    @Test
    void shortenShouldPersistAndCacheUrl() {
        when(shortCodeGenerator.generate()).thenReturn(CODE);
        when(urlRepository.existsByShortCode(CODE)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUrlResponse resp = urlService.shorten(new CreateUrlRequest(URL_STR, null));

        assertEquals(CODE, resp.shortCode());
        assertEquals("http://localhost:8080/" + CODE, resp.shortUrl());
        assertEquals(URL_STR, resp.originalUrl());

        verify(urlRepository).save(any(Url.class));
        verify(cacheService).putRedirection(eq(CODE), eq(URL_STR), any(Duration.class));
    }

    @Test
    void shortenShouldRetryOnCollision() {
        when(urlRepository.existsByShortCode("collide")).thenReturn(true);
        when(shortCodeGenerator.generate())
                .thenReturn("collide")
                .thenReturn(CODE);
        when(urlRepository.existsByShortCode(CODE)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUrlResponse resp = urlService.shorten(new CreateUrlRequest(URL_STR, 60));

        assertEquals(CODE, resp.shortCode());
        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void resolveShouldReturnCachedUrlWithoutDbLookup() {
        when(cacheService.getRedirection(CODE)).thenReturn(Optional.of(URL_STR));
        Url persisted = sampleUrl();
        when(urlRepository.findByShortCode(CODE)).thenReturn(Optional.of(persisted));

        String resolved = urlService.resolve(CODE);

        assertEquals(URL_STR, resolved);
        ArgumentCaptor<Url> captor = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getAccessCount());
    }

    @Test
    void resolveShouldCacheOnMissAndPersistAccess() {
        when(cacheService.getRedirection(CODE)).thenReturn(Optional.empty());
        Url persisted = sampleUrl();
        when(urlRepository.findByShortCode(CODE)).thenReturn(Optional.of(persisted));
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accessLogRepository.findByUrlIdAndAccessDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        String resolved = urlService.resolve(CODE);

        assertEquals(URL_STR, resolved);
        verify(cacheService).putRedirection(eq(CODE), eq(URL_STR), any(Duration.class));
        verify(accessLogRepository).save(any(AccessLog.class));
    }

    @Test
    void resolveShouldThrowWhenExpired() {
        when(cacheService.getRedirection(CODE)).thenReturn(Optional.of(URL_STR));
        Url expired = sampleUrl();
        expired.setExpiresAt(Instant.now().minusSeconds(10));
        when(urlRepository.findByShortCode(CODE)).thenReturn(Optional.of(expired));

        assertThrows(UrlNotFoundException.class, () -> urlService.resolve(CODE));
        verify(cacheService).evictRedirection(CODE);
    }

    @Test
    void resolveShouldThrowWhenNotFound() {
        when(cacheService.getRedirection(CODE)).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(CODE)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.resolve(CODE));
    }

    @Test
    void getStatsShouldAggregateAccessByDay() {
        Url persisted = sampleUrl();
        persisted.setAccessCount(42);
        when(urlRepository.findByShortCode(CODE)).thenReturn(Optional.of(persisted));
        
        Object[] row1 = new Object[]{LocalDate.now(), 10L};
        Object[] row2 = new Object[]{LocalDate.now().minusDays(1), 32L};
        when(accessLogRepository.findDailyStatsByUrlId(persisted.getId())).thenReturn(List.of(row1, row2));

        UrlStatsResponse stats = urlService.getStats(CODE);

        assertEquals(CODE, stats.shortCode());
        assertEquals(42, stats.totalAccesses());
        assertEquals(2, stats.dailyAccesses().size());
    }

    @Test
    void deactivateExpiredShouldDeactivateAndEvictCache() {
        Url u1 = sampleUrl();
        u1.setExpiresAt(Instant.now().minusSeconds(60));
        Url u2 = sampleUrl();
        u2.setShortCode("xyz9876");
        u2.setExpiresAt(Instant.now().minusSeconds(60));
        when(urlRepository.findAllByActiveTrueAndExpiresAtBefore(any(Instant.class))).thenReturn(List.of(u1, u2));
        when(urlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int count = urlService.deactivateExpired();

        assertEquals(2, count);
        assertFalse(u1.isActive());
        assertFalse(u2.isActive());
        verify(cacheService).evictRedirection(u1.getShortCode());
        verify(cacheService).evictRedirection(u2.getShortCode());
    }

    private Url sampleUrl() {
        Instant now = Instant.now();
        Url url = new Url(URL_STR, CODE, now, now.plusSeconds(3600));
        url.setId(1L);
        return url;
    }
}