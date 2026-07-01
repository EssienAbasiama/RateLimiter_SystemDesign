package com.ratelimiter.server;

/**
 * Pure helpers for computing rate-limit response header values.
 *
 * <p>Kept separate from {@link RateLimiterMiddleware} (which depends on the Spark
 * HTTP types) so the arithmetic can be unit-tested in isolation.
 */
public final class RateLimitHeaders {

    private RateLimitHeaders() {
    }

    /**
     * Seconds until the bucket would refill back to full capacity.
     * Used for the {@code X-RateLimit-Reset} header.
     */
    public static long resetSeconds(int capacity, double remainingTokens, double refillRatePerSecond) {
        if (refillRatePerSecond <= 0) return 0;
        double tokensNeeded = capacity - remainingTokens;
        if (tokensNeeded <= 0) return 0;
        return (long) (tokensNeeded / refillRatePerSecond);
    }

    /**
     * Seconds until at least one token becomes available again, rounded up so a
     * client never retries a fraction of a second too early.
     * Used for the {@code Retry-After} header.
     */
    public static long retryAfterSeconds(double remainingTokens, double refillRatePerSecond) {
        if (refillRatePerSecond <= 0) return 0;
        double deficit = 1.0 - remainingTokens;
        if (deficit <= 0) return 0;
        return (long) Math.ceil(deficit / refillRatePerSecond);
    }
}
