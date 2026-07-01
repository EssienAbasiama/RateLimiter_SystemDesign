package com.ratelimiter.server;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.IdentifierProvider;
import com.ratelimiter.model.RateLimitResult;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
public class RateLimiterMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterMiddleware.class);
    private static final Gson gson = new Gson();
    
    private final RateLimiter rateLimiter;
    
    public RateLimiterMiddleware(RateLimiter rateLimiter) {
        if (rateLimiter == null) {
            throw new IllegalArgumentException("RateLimiter cannot be null");
        }
        this.rateLimiter = rateLimiter;
    }
    
    public boolean checkRateLimit(
        Request request,
        Response response,
        IdentifierProvider identifierProvider
    ){
        String identifier=identifierProvider.getIdentifier();
        logger.info("Checking rate limit for identifier: {}", identifier);
        RateLimitResult result=rateLimiter.tryConsume(identifier);
        if(!result.isAllowed()){
            // Rate limit exceeded - set 429 status
            long retryAfter = RateLimitHeaders.retryAfterSeconds(
                result.getRemainingTokens(), rateLimiter.getRefillRatePerSecond());
            response.status(429);
            response.type("application/json");
            // Standard header so well-behaved clients back off for the right amount of time.
            response.header("Retry-After", String.valueOf(retryAfter));
            Map<String , Object>errorResponse=new HashMap<>();
            errorResponse.put("error", "Rate limit exceeded");
            errorResponse.put("message", "Too many requests. Please try again later.");
            errorResponse.put("remainingTokens", result.getRemainingTokens());
            errorResponse.put("capacity", result.getCapacity());
            errorResponse.put("retryAfterSeconds", retryAfter);
            response.body(gson.toJson(errorResponse));

            logger.warn("Rate limit exceeded for identifier: {}", identifier);
            return false;
        }
        // Add rate limit headers to response
        response.header("X-RateLimit-Remaining", String.valueOf((int)result.getRemainingTokens()));
        response.header("X-RateLimit-Capacity", String.valueOf(result.getCapacity()));
        response.header("X-RateLimit-Reset", String.valueOf(
            RateLimitHeaders.resetSeconds(rateLimiter.getCapacity(),
                result.getRemainingTokens(), rateLimiter.getRefillRatePerSecond())));

        logger.debug("Request allowed for identifier: {}", identifier);
        return true;
    }
}

