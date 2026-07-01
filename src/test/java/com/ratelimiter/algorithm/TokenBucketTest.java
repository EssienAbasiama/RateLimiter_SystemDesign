package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.storage.InMemoryStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    void allowsUpToCapacityThenRejects() {
        // capacity 5, effectively no refill during the test, 1 token per request
        TokenBucket limiter = new TokenBucket(storage, 5, 0.0001, 1);

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryConsume("user:a").isAllowed(), "request " + i + " should be allowed");
        }
        assertFalse(limiter.tryConsume("user:a").isAllowed(), "6th request should be rejected");
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        // capacity 2, refills 10 tokens/sec -> a full bucket comes back quickly
        TokenBucket limiter = new TokenBucket(storage, 2, 10.0, 1);

        assertTrue(limiter.tryConsume("user:b").isAllowed());
        assertTrue(limiter.tryConsume("user:b").isAllowed());
        assertFalse(limiter.tryConsume("user:b").isAllowed(), "bucket should be empty");

        Thread.sleep(250); // ~2.5 tokens refilled

        assertTrue(limiter.tryConsume("user:b").isAllowed(), "should be allowed after refill");
    }

    @Test
    void differentIdentifiersHaveIndependentBuckets() {
        TokenBucket limiter = new TokenBucket(storage, 1, 0.0001, 1);

        assertTrue(limiter.tryConsume("user:x").isAllowed());
        assertFalse(limiter.tryConsume("user:x").isAllowed());
        // A different identifier still has a full bucket.
        assertTrue(limiter.tryConsume("user:y").isAllowed());
    }

    @Test
    void remainingTokensAreReported() {
        TokenBucket limiter = new TokenBucket(storage, 3, 0.0001, 1);

        RateLimitResult first = limiter.tryConsume("user:c");
        assertTrue(first.isAllowed());
        assertEquals(2.0, first.getRemainingTokens(), 0.01);
        assertEquals(3, first.getCapacity());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(null, 5, 1.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(storage, 0, 1.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(storage, 5, 0.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(storage, 5, 1.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(storage, 5, 1.0, 6));
    }

    @Test
    void rejectsInvalidIdentifier() {
        TokenBucket limiter = new TokenBucket(storage, 5, 1.0, 1);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryConsume(null));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryConsume(""));
    }

    /**
     * The core guarantee: under heavy concurrency on a single identifier, the
     * number of allowed requests must never exceed the bucket capacity. This is
     * exactly what the old non-atomic read-modify-write implementation failed.
     */
    @Test
    void concurrentRequestsNeverExceedCapacity() throws InterruptedException {
        int capacity = 100;
        int threads = 50;
        int requestsPerThread = 20; // 1000 total attempts, only 100 should pass
        // Negligible refill so capacity is the hard ceiling for the test duration.
        TokenBucket limiter = new TokenBucket(storage, capacity, 0.0001, 1);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger allowed = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    if (limiter.tryConsume("user:hot").isAllowed()) {
                        allowed.incrementAndGet();
                    }
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "workers did not finish in time");

        assertEquals(capacity, allowed.get(),
            "exactly capacity requests should be allowed; got " + allowed.get());
    }
}
