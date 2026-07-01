package com.ratelimiter.algorithm;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.Storage;

/**
 * TokenBucketRateLimiter is an alias for TokenBucket
 * This class provides a more descriptive name for the rate limiter implementation
 */
public class TokenBucketRateLimiter extends TokenBucket implements RateLimiter {
    
    public TokenBucketRateLimiter(
        Storage storage,
        int capacity,
        double refillRatePerSecond,
        int tokensRequired
    ) {
        super(storage, capacity, refillRatePerSecond, tokensRequired);
    }
}

