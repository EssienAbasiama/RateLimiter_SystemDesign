package com.ratelimiter.core;

import com.ratelimiter.model.RateLimitResult;

public interface RateLimiter {
    RateLimitResult tryConsume(String identifier);
    int getCapacity();
    double getRefillRatePerSecond();
}
