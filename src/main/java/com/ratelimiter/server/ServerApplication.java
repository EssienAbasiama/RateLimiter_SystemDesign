package com.ratelimiter.server;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimiterConfig;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.provider.RequestIdentifierProvider;
import com.ratelimiter.core.Storage;
import com.ratelimiter.storage.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import static spark.Spark.*;
public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    
    private final RateLimiterConfig config;
    private final Storage storage;
    private final RateLimiter rateLimiter;
    private final RateLimiterMiddleware middleware;

    public ServerApplication(RateLimiterConfig config) {
        this.config = config;
        this.storage = StorageFactory.createStorage(config);
        this.rateLimiter = new TokenBucketRateLimiter(
            storage,
            config.getCapacity(),
            config.getRefillRatePerSecond(),
            config.getTokensRequired()
        );
        this.middleware = new RateLimiterMiddleware(rateLimiter);
    }

 /**
     * Starts the HTTP server
     */
 public void start() {
    port(config.getServerPort());
    
    // Apply rate limiter middleware to all routes
    before((request, response) -> {
        RequestIdentifierProvider provider = new RequestIdentifierProvider(request);
        if (!middleware.checkRateLimit(request, response, provider)) {
            halt(429);  // Stop request processing
        }
    });
    
    // Sample API endpoints
    get("/api/hello", (req, res) -> {
        res.type("application/json");
        return "{\"message\":\"Hello! Request allowed.\"}";
    });
    
    get("/api/data", (req, res) -> {
        res.type("application/json");
        return "{\"data\":\"Sample data response\",\"timestamp\":" + System.currentTimeMillis() + "}";
    });
    
    post("/api/echo", (req, res) -> {
        res.type("application/json");
        return "{\"echo\":\"" + req.body() + "\"}";
    });
    
    get("/api/health", (req, res) -> {
        res.type("application/json");
        return "{\"status\":\"healthy\",\"storage\":\"" + config.getStorageType() +
               "\",\"rateLimiter\":{\"capacity\":" +
               rateLimiter.getCapacity() + ",\"refillRate\":" +
               rateLimiter.getRefillRatePerSecond() + "}}";
    });

    logger.info("Server started on port {}", config.getServerPort());
    logger.info("Rate limiter configured: capacity={}, refillRate={}/sec, tokensRequired={}", 
               config.getCapacity(), config.getRefillRatePerSecond(), config.getTokensRequired());
}

/**
     * Stops the HTTP server and closes storage
     */
public void stop() {
    Spark.stop();
    storage.close();
    logger.info("Server stopped");
}

}
