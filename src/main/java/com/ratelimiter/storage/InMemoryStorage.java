package com.ratelimiter.storage;

import com.ratelimiter.core.Storage;
import com.ratelimiter.model.ConsumeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process {@link Storage} backed by a {@link ConcurrentHashMap}.
 *
 * <p>Useful for local development and tests (no Redis required). The refill +
 * check + consume step is made atomic per-identifier via
 * {@link ConcurrentHashMap#compute}, mirroring the atomicity that the Redis
 * backend gets from its Lua script.
 *
 * <p>Note: state lives only in this JVM, so this backend is <b>not</b> suitable
 * for a multi-instance deployment — use {@link RedisStorage} there.
 */
public class InMemoryStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);

    private static final class Bucket {
        final double tokens;
        final long lastRefillTimestamp;

        Bucket(double tokens, long lastRefillTimestamp) {
            this.tokens = tokens;
            this.lastRefillTimestamp = lastRefillTimestamp;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryStorage() {
        logger.info("InMemoryStorage initialized");
    }

    @Override
    public ConsumeResult tryConsume(String identifier, int capacity, double refillRatePerSecond, int tokensRequired) {
        long now = System.currentTimeMillis();
        // Holder lets us read the decision made inside the atomic compute block.
        boolean[] allowed = {false};

        buckets.compute(identifier, (key, existing) -> {
            double tokens = existing == null ? capacity : existing.tokens;
            long lastTs = existing == null ? now : existing.lastRefillTimestamp;

            double elapsedSeconds = Math.max(0, now - lastTs) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);

            if (tokens >= tokensRequired) {
                tokens -= tokensRequired;
                allowed[0] = true;
            }
            return new Bucket(tokens, now);
        });

        double remaining = buckets.get(identifier).tokens;
        return new ConsumeResult(allowed[0], remaining);
    }

    @Override
    public void close() {
        buckets.clear();
        logger.info("InMemoryStorage closed");
    }
}
