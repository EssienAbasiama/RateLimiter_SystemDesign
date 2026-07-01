package com.ratelimiter.model;

/**
 * Outcome of an atomic refill-and-consume operation against the storage backend.
 *
 * <p>Unlike the previous read-then-write flow, the whole decision (refill +
 * check + consume) is made in a single atomic step, so {@code allowed} and
 * {@code remainingTokens} are guaranteed to be mutually consistent.
 */
public final class ConsumeResult {
    private final boolean allowed;
    private final double remainingTokens;

    public ConsumeResult(boolean allowed, double remainingTokens) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public double getRemainingTokens() {
        return remainingTokens;
    }

    @Override
    public String toString() {
        return String.format("ConsumeResult{allowed=%s, remainingTokens=%.2f}", allowed, remainingTokens);
    }
}
