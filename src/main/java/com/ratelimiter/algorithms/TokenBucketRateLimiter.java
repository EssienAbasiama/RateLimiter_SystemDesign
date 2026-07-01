package com.ratelimiter.algorithm;

import com.ratelimiter.core.Storage;

/**
 * Descriptive alias for {@link TokenBucket}. Kept for readability at call sites;
 * {@code RateLimiter} is already inherited from the superclass.
 */
public class TokenBucketRateLimiter extends TokenBucket {


    public TokenBucketRateLimiter(
        Storage storage,
        int capacity,
        double refillRatePerSecond,
        int tokensRequired
    ) {
        super(storage, capacity, refillRatePerSecond, tokensRequired);
    }
}

