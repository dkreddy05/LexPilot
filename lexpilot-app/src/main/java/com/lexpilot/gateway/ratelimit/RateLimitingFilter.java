package com.lexpilot.gateway.ratelimit;

import com.lexpilot.common.config.AppConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-IP rate limiting using Bucket4j (in-memory) with memory-leak protection.
 * <p>
 * Activated only when {@code lexpilot.security.enabled=true}.
 * Reads {@code lexpilot.rate-limiting.requests-per-minute} from config.
 * Returns 429 Too Many Requests with a Retry-After header when the limit is exceeded.
 */
@Component
@ConditionalOnProperty(name = "lexpilot.security.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_BUCKETS = 25_000;
    private static final long EVICTION_INTERVAL_MS = 60_000; // 1 minute
    private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

    private record CachedBucket(Bucket bucket, AtomicLong lastAccessTimestamp) {}

    private final ConcurrentMap<String, CachedBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastEvictionTime = new AtomicLong(System.currentTimeMillis());
    private final int requestsPerMinute;

    public RateLimitingFilter(AppConfig appConfig) {
        this.requestsPerMinute = appConfig.rateLimiting() != null
                ? appConfig.rateLimiting().requestsPerMinute()
                : 60; // sensible default
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        maybeEvictStaleBuckets();

        CachedBucket cachedBucket = buckets.compute(clientIp, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null) {
                if (buckets.size() >= MAX_BUCKETS) {
                    evictOldestEntries();
                }
                return new CachedBucket(createBucket(), new AtomicLong(now));
            }
            existing.lastAccessTimestamp().set(now);
            return existing;
        });

        ConsumptionProbe probe = cachedBucket.bucket().tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after "
                            + waitSeconds + " seconds.\"}");
            return;
        }

        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.equals("/actuator/info") || path.equals("/") || path.equals("/error");
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(requestsPerMinute)
                                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                                .build()
                )
                .build();
    }

    /**
     * Resolves the client IP safely.
     * Raw X-Forwarded-For headers from untrusted clients are not blindly trusted;
     * uses getRemoteAddr() as the secure default to prevent spoofing.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }

    private void maybeEvictStaleBuckets() {
        long now = System.currentTimeMillis();
        long last = lastEvictionTime.get();
        if (now - last > EVICTION_INTERVAL_MS && lastEvictionTime.compareAndSet(last, now)) {
            long threshold = now - BUCKET_TTL.toMillis();
            int removed = 0;
            Iterator<Map.Entry<String, CachedBucket>> it = buckets.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CachedBucket> entry = it.next();
                if (entry.getValue().lastAccessTimestamp().get() < threshold) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                log.debug("Evicted {} stale rate-limiting buckets (remaining: {})", removed, buckets.size());
            }
        }
    }

    private void evictOldestEntries() {
        long threshold = System.currentTimeMillis() - (BUCKET_TTL.toMillis() / 2);
        Iterator<Map.Entry<String, CachedBucket>> it = buckets.entrySet().iterator();
        while (it.hasNext() && buckets.size() > (MAX_BUCKETS * 0.8)) {
            Map.Entry<String, CachedBucket> entry = it.next();
            if (entry.getValue().lastAccessTimestamp().get() < threshold) {
                it.remove();
            }
        }
    }
}
