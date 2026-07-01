package com.ratelimiter.model;

public final class RateLimitResult {
    private final boolean allowed;
    private final double remainingTokens;
    private final int capacity;
    private final String identifier;

    public RateLimitResult(boolean allowed, double remainingTokens, int capacity, String identifier){
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
        this.identifier = identifier;
    }
    public boolean isAllowed(){
        return allowed;
    }
    public double getRemainingTokens(){
        return remainingTokens;
    }
    public int getCapacity(){
        return capacity;
    }
    public String getIdentifier(){
        return identifier;
    }
    @Override
    public String toString() {
        return String.format("RateLimitResult{allowed=%s, remaining=%.2f, capacity=%d, id='%s'}", 
                           allowed, remainingTokens, capacity, identifier);
    }
}
