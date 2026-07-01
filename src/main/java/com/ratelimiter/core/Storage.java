package com.ratelimiter.core;

import com.ratelimiter.model.ConsumeResult;

/**
 * Storage backend for rate-limiter state.
 *
 * <p>The backend is responsible for performing the refill-and-consume step
 * <b>atomically</b>. Exposing a single {@link #tryConsume} operation (rather
 * than separate get/update methods) is what makes the limiter correct under
 * concurrency and across multiple server instances sharing the same store.
 */
public interface Storage {

    /**
     * Atomically refills the bucket for {@code identifier} based on elapsed time,
     * then consumes {@code tokensRequired} tokens if enough are available.
     *
     * @param identifier          the bucket key (e.g. {@code "ip:1.2.3.4"})
     * @param capacity            maximum tokens the bucket can hold
     * @param refillRatePerSecond tokens added per second
     * @param tokensRequired      tokens this request needs
     * @return the resulting state and whether the request was allowed
     */
    ConsumeResult tryConsume(String identifier, int capacity, double refillRatePerSecond, int tokensRequired);

    void close();
}
