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

    @Transactional
    public CreateUrlResponse shorten(CreateUrlRequest request) {
        int expirationMinutes = request.expirationMinutes() != null
                ? request.expirationMinutes()
                : defaultExpirationMinutes;

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));

        String shortCode = generateUniqueShortCode();
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

    @Transactional
    public int deactivateExpired() {
        int deactivated = urlRepository.deactivateExpired(Instant.now());
        log.info("Deactivated {} expired URLs", deactivated);
        return deactivated;
    }

    @Transactional
    public void delete(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        url.setActive(false);
        urlRepository.save(url);
        cacheService.evictRedirection(shortCode);
        log.info("Deleted (deactivated) short URL shortCode={}", shortCode);
    }

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