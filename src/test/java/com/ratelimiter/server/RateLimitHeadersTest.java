package com.ratelimiter.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitHeadersTest {

    @Test
    void resetSecondsCountsTimeToRefillToCapacity() {
        // 10 capacity, 3 remaining, 1 token/sec -> 7 tokens missing -> 7 seconds
        assertEquals(7, RateLimitHeaders.resetSeconds(10, 3.0, 1.0));
    }

    @Test
    void resetSecondsIsZeroWhenFull() {
        assertEquals(0, RateLimitHeaders.resetSeconds(10, 10.0, 1.0));
    }

    @Test
    void resetSecondsIsZeroWhenRefillRateNonPositive() {
        assertEquals(0, RateLimitHeaders.resetSeconds(10, 0.0, 0.0));
    }

    @Test
    void retryAfterRoundsUp() {
        // 0.2 tokens left, need 1 -> deficit 0.8, at 2/sec -> 0.4s -> ceil -> 1
        assertEquals(1, RateLimitHeaders.retryAfterSeconds(0.2, 2.0));
    }

    @Test
    void retryAfterScalesWithSlowRefill() {
        // deficit 1.0 at 0.5/sec -> 2s
        assertEquals(2, RateLimitHeaders.retryAfterSeconds(0.0, 0.5));
    }

    @Test
    void retryAfterIsZeroWhenTokenAlreadyAvailable() {
        assertEquals(0, RateLimitHeaders.retryAfterSeconds(1.0, 1.0));
    }
}
