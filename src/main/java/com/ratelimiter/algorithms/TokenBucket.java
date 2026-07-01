package com.ratelimiter.algorithm;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.Storage;
import com.ratelimiter.model.ConsumeResult;
import com.ratelimiter.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Token bucket rate limiter.
 *
 * The refill + check + consume steps run atomically inside the storage backend
 * (see Storage#tryConsume), so this class only owns configuration/validation and
 * shaping the result. This avoids the read-modify-write race that would otherwise
 * let concurrent requests exceed the configured limit.
 */
public class TokenBucket implements RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucket.class);
    private final Storage storage;
    private final int capacity;
    private final double refillRatePerSecond;
    private final int tokensRequired;

    public TokenBucket(Storage storage, int capacity, double refillRatePerSecond, int tokensRequired) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("Refill rate must be positive");
        }
        if (tokensRequired <= 0) {
            throw new IllegalArgumentException("Tokens required must be positive");
        }
        if (tokensRequired > capacity) {
            throw new IllegalArgumentException("Tokens required cannot exceed capacity");
        }
        this.storage = storage;
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokensRequired = tokensRequired;
        logger.info("TokenBucket initialized with capacity={}, refillRatePerSecond={}, tokensRequired={}",
            capacity, refillRatePerSecond, tokensRequired);
    }

    @Override
    public RateLimitResult tryConsume(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        ConsumeResult result = storage.tryConsume(identifier, capacity, refillRatePerSecond, tokensRequired);

        if (result.isAllowed()) {
            logger.debug("Allowed request for identifier={}, remaining={}", identifier, result.getRemainingTokens());
        } else {
            logger.info("Rate limit exceeded for identifier={}, required={}, available={}",
                identifier, tokensRequired, result.getRemainingTokens());
        }
        return new RateLimitResult(result.isAllowed(), result.getRemainingTokens(), capacity, identifier);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public double getRefillRatePerSecond() {
        return refillRatePerSecond;
    }
}
