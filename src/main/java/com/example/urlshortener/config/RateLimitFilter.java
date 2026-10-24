package com.example.urlshortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String CREATE_PATH = "/api/urls";

    private final ProxyManager<String> proxyManager;
    private final long capacity;
    private final long refillTokens;
    private final long refillPeriodSeconds;

    public RateLimitFilter(
            RedisClient redisClient,
            @Value("${app.rate-limit.capacity:10}") long capacity,
            @Value("${app.rate-limit.refill-tokens:1}") long refillTokens,
            @Value("${app.rate-limit.refill-period-seconds:6}") long refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodSeconds = refillPeriodSeconds;

        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(10)))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(req.getMethod()) || !req.getRequestURI().equals(CREATE_PATH)) {
            chain.doFilter(req, res);
            return;
        }

        String ip = clientIp(req);
        Bucket bucket = proxyManager.builder().build("ratelimit:" + ip, this::newBucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            res.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(req, res);
        } else {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            log.warn("Rate limit exceeded for IP {}", ip);
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(waitSeconds));
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String message = "Too many requests. Try again in " + waitSeconds + "s.";
            res.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                    + "\"error\":\"Too Many Requests\",\"message\":\"" + message + "\"}");
            res.getWriter().flush();
        }
    }

    private BucketConfiguration newBucketConfiguration() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, Duration.ofSeconds(refillPeriodSeconds))
                .build();
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
