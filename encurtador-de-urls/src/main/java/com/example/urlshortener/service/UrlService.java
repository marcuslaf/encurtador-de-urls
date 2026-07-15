package com.example.urlshortener.service;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.CreateUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.Url;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.AccessLogRepository;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.ShortCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core service for URL shortening operations.
 *
 * <p>Handles URL creation with optional custom aliases, resolution with Redis caching,
 * access tracking with atomic upserts, and automatic expiration management.</p>
 *
 * <p>Thread safety: Uses {@code @Transactional} for database consistency and
 * atomic PostgreSQL upserts for concurrent access counting.</p>
 *
 * @see UrlRepository
 * @see CacheService
 * @see ShortCodeGenerator
 */
@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);
    private static final int MAX_COLLISION_RETRIES = 10;

    private final UrlRepository urlRepository;
    private final AccessLogRepository accessLogRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CacheService cacheService;
    private final String baseUrl;
    private final int defaultExpirationMinutes;

    public UrlService(
            UrlRepository urlRepository,
            AccessLogRepository accessLogRepository,
            ShortCodeGenerator shortCodeGenerator,
            CacheService cacheService,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.default-expiration-minutes:1440}") int defaultExpirationMinutes) {
        this.urlRepository = urlRepository;
        this.accessLogRepository = accessLogRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.cacheService = cacheService;
        this.baseUrl = baseUrl;
        this.defaultExpirationMinutes = defaultExpirationMinutes;
    }

    /**
     * Creates a new shortened URL.
     *
     * @param request containing the original URL, optional expiration, and optional custom alias
     * @return the created short URL response with short code, full URL, and timestamps
     * @throws IllegalArgumentException if custom alias is already in use
     */
    @Transactional
    public CreateUrlResponse shorten(CreateUrlRequest request) {
        int expirationMinutes = request.expirationMinutes() != null
                ? request.expirationMinutes()
                : defaultExpirationMinutes;

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));

        String shortCode;
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            if (urlRepository.existsByShortCode(request.customAlias())) {
                throw new IllegalArgumentException("Custom alias already in use: " + request.customAlias());
            }
            shortCode = request.customAlias();
        } else {
            shortCode = generateUniqueShortCode();
        }

        Url url = new Url(request.originalUrl(), shortCode, now, expiresAt);

        Url saved = urlRepository.save(url);
        log.info("Created short URL id={} shortCode={} expiresAt={}", saved.getId(), shortCode, expiresAt);

        Duration ttl = Duration.between(now, expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }
        cacheService.putRedirection(shortCode, request.originalUrl(), ttl);

        return new CreateUrlResponse(
                shortCode,
                baseUrl + "/" + shortCode,
                saved.getOriginalUrl(),
                saved.getCreatedAt(),
                saved.getExpiresAt());
    }

    /**
     * Resolves a short code to its original URL.
     *
     * <p>Checks Redis cache first, falls back to PostgreSQL on cache miss.
     * Automatically increments access counter and logs the access.</p>
     *
     * @param shortCode the short code to resolve
     * @return the original URL
     * @throws UrlNotFoundException if the URL is not found or has expired
     */
    @Transactional
    public String resolve(String shortCode) {
        String cached = cacheService.getRedirection(shortCode).orElse(null);
        if (cached != null) {
            Url url = urlRepository.findByShortCode(shortCode).orElse(null);
            if (url != null && url.isActive() && !url.isExpired()) {
                registerAccess(url);
                return cached;
            }
            cacheService.evictRedirection(shortCode);
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));

        if (!url.isActive() || url.isExpired()) {
            throw new UrlNotFoundException("URL not found or expired: " + shortCode);
        }

        Duration ttl = Duration.between(Instant.now(), url.getExpiresAt());
        if (ttl.isPositive()) {
            cacheService.putRedirection(shortCode, url.getOriginalUrl(), ttl);
        }
        registerAccess(url);
        return url.getOriginalUrl();
    }

    /**
     * Retrieves access statistics for a short URL.
     *
     * @param shortCode the short code to get stats for
     * @return statistics including total accesses and daily breakdown
     * @throws UrlNotFoundException if the URL is not found
     */
    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));

        List<UrlStatsResponse.DailyAccess> daily = new ArrayList<>();
        List<Object[]> results = accessLogRepository.findDailyStatsByUrlId(url.getId());
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            long count = ((Number) row[1]).longValue();
            daily.add(new UrlStatsResponse.DailyAccess(date, count));
        }

        long total = url.getAccessCount();
        return new UrlStatsResponse(shortCode, total, daily);
    }

    /**
     * Deactivates all expired URLs using a bulk database update.
     *
     * <p>Called periodically by {@link com.example.urlshortener.scheduler.ExpirationScheduler}.
     * Uses atomic JPQL update to avoid loading all expired URLs into memory.</p>
     *
     * @return number of URLs deactivated
     */
    @Transactional
    public int deactivateExpired() {
        int deactivated = urlRepository.deactivateExpired(Instant.now());
        log.info("Deactivated {} expired URLs", deactivated);
        return deactivated;
    }

    /**
     * Soft-deletes a short URL by deactivating it.
     *
     * @param shortCode the short code to delete
     * @throws UrlNotFoundException if the URL is not found
     */
    @Transactional
    public void delete(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        url.setActive(false);
        urlRepository.save(url);
        cacheService.evictRedirection(shortCode);
        log.info("Deleted (deactivated) short URL shortCode={}", shortCode);
    }

    /**
     * Lists all active short URLs with pagination.
     *
     * @param page zero-based page index
     * @param size number of items per page
     * @return paginated list of short URL responses
     */
    @Transactional(readOnly = true)
    public Page<CreateUrlResponse> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return urlRepository.findAllByActiveTrueOrderByCreatedAtDesc(pageable)
                .map(u -> new CreateUrlResponse(
                        u.getShortCode(),
                        baseUrl + "/" + u.getShortCode(),
                        u.getOriginalUrl(),
                        u.getCreatedAt(),
                        u.getExpiresAt()));
    }

    /**
     * Gets the full short URL for a given short code.
     *
     * @param shortCode the short code
     * @return the full short URL (e.g., http://localhost:8080/abc1234)
     * @throws UrlNotFoundException if the URL is not found or inactive
     */
    @Transactional(readOnly = true)
    public String getShortUrl(String shortCode) {
        Url url = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        return baseUrl + "/" + url.getShortCode();
    }

    private void registerAccess(Url url) {
        url.setAccessCount(url.getAccessCount() + 1);
        urlRepository.save(url);
        accessLogRepository.upsertAccessLog(url.getId(), LocalDate.now());
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            String code = shortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        log.warn("Collision retries exhausted - falling back to UUID-based code");
        return shortCodeGenerator.generate() + "-" + UUID.randomUUID().toString().substring(0, 4);
    }
}