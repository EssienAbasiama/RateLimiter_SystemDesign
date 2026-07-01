package com.ratelimiter.storage;

import com.ratelimiter.model.ConsumeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStorageTest {

    @Test
    void firstCallStartsFromFullCapacity() {
        InMemoryStorage storage = new InMemoryStorage();
        ConsumeResult result = storage.tryConsume("k", 10, 0.0001, 1);
        assertTrue(result.isAllowed());
        assertEquals(9.0, result.getRemainingTokens(), 0.01);
    }

    @Test
    void doesNotRefillBeyondCapacity() throws InterruptedException {
        InMemoryStorage storage = new InMemoryStorage();
        // Consume one, then wait long enough that naive refill would overflow.
        storage.tryConsume("k", 5, 1000.0, 1);
        Thread.sleep(50);
        ConsumeResult result = storage.tryConsume("k", 5, 1000.0, 1);
        // After refill the bucket is capped at capacity (5), minus the 1 just taken.
        assertEquals(4.0, result.getRemainingTokens(), 0.01);
    }

    @Test
    void rejectsWhenInsufficientTokens() {
        InMemoryStorage storage = new InMemoryStorage();
        assertTrue(storage.tryConsume("k", 1, 0.0001, 1).isAllowed());
        assertFalse(storage.tryConsume("k", 1, 0.0001, 1).isAllowed());
    }
}
