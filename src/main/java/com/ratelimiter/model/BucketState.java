package com.ratelimiter.model;

public final class BucketState {
    private final double tokens;
    private final long lastRefillTimestamp;
    public BucketState(double tokens, long lastRefillTimestamp){
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }
    public double getTokens(){
        return tokens;
    }
    public long getLastRefillTimestamp(){
        return lastRefillTimestamp;
    }
    public static BucketState initial(int capacity){
        return new BucketState(capacity, System.currentTimeMillis());
    }
    @Override
    public String toString(){
        return String.format("BucketState(tokens=%f, lastRefillTimestamp=%d)", tokens, lastRefillTimestamp);
    }
}
