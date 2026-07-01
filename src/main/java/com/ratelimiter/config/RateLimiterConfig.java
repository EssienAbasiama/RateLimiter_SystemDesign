package com.ratelimiter.config;

import java.util.Properties;

public final class RateLimiterConfig {
    private final int capacity;
    private final double refillRatePerSecond;
    private final int tokensRequired;
    private final String storageType;
    private final String redisHost;
    private final int redisPort;
    private final int serverPort;


    public RateLimiterConfig() {
        Properties props = ConfigLoader.loadProperties();
        
        // Token bucket parameters
        this.capacity = Integer.parseInt(props.getProperty("rate.limiter.capacity", "10"));
        this.refillRatePerSecond = Double.parseDouble(
        props.getProperty("rate.limiter.refill.rate.per.second", "2.0"));
        this.tokensRequired = Integer.parseInt(props.getProperty("rate.limiter.tokens.required", "1"));
        
        // Storage configuration
        this.storageType = props.getProperty("storage.type", "redis");
        
        // Redis configuration
        this.redisHost = props.getProperty("redis.host", "localhost");
        this.redisPort = Integer.parseInt(props.getProperty("redis.port", "6379"));
        
        // Server configuration
        this.serverPort = Integer.parseInt(props.getProperty("server.port", "8080"));
    }
        // Getters
        public int getCapacity() { return capacity; }
        public double getRefillRatePerSecond() { return refillRatePerSecond; }
        public int getTokensRequired() { return tokensRequired; }
        public String getStorageType() { return storageType; }
        public String getRedisHost() { return redisHost; }
        public int getRedisPort() { return redisPort; }
        public int getServerPort() { return serverPort; }
}
