package com.ratelimiter.algorithm;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.Storage;
import com.ratelimiter.model.BucketState;
import com.ratelimiter.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 1. Identify user → generate bucket key 
 * 2. Fetch tokens + last refill timestamp from Redis
 * 3. Compute elapsed time
 * 4. Compute refill token amount
 * 5. Update current token count
 * 6. Check if enough tokens exist
 * 7. If allowed → consume tokens
 * 8. Save updated token count + timestamp
 * 9. Return allowed/denied
*/

public class TokenBucket implements RateLimiter{
    private static final Logger logger=LoggerFactory.getLogger(TokenBucket.class);
    private final Storage storage;
    private final int capacity;
    private final double refillRatePerSecond;
    private final int tokensRequired;
    public TokenBucket(
        Storage storage,int capacity, double refillRatePerSecond, int tokensRequired
    ){
    if (storage == null) {
        throw new IllegalArgumentException("Storage cannot be null");
    }
    if (capacity <= 0) {
        throw new IllegalArgumentException("Capacity must be positive");
    }
    if (refillRatePerSecond <= 0) {
        throw new IllegalArgumentException("Refill rate must be positive");
    }
    if (tokensRequired <= 0) {
        throw new IllegalArgumentException("Tokens required must be positive");
    }
    this.storage = storage;
    this.capacity = capacity;
    this.refillRatePerSecond = refillRatePerSecond;
    this.tokensRequired = tokensRequired;
    logger.info("TokenBucket initialized with capacity={}, refillRatePerSecond={}, tokensRequired={}", capacity, refillRatePerSecond, tokensRequired);

}
@Override
public RateLimitResult tryConsume(String identifier){
    if(identifier==null||identifier.isEmpty()){
        logger.error("Identifier cannot be null or empty");
        throw new IllegalArgumentException("Identifier cannot be null or empty");
    }
    long currentTime = System.currentTimeMillis();
    //fetch current state from storage
    BucketState currentState = storage.getBucketState(identifier);
    if(currentState==null){
        currentState = BucketState.initial(capacity);
        logger.info("Initial bucket state for identifier={}: {}", identifier, currentState);

    }
    //calculate time elapsed from last refill (convert to seconds)
    long elapsedTime = currentTime - currentState.getLastRefillTimestamp();
    double elapsedSeconds = elapsedTime / 1000.0;
    //how many tokens should be refilled?
    double refillAmount = elapsedSeconds * refillRatePerSecond;
    //update
    double updatedTokens = Math.min(currentState.getTokens() + refillAmount, capacity);
    //check if enough tokens are available
    if(updatedTokens < tokensRequired){
        logger.info("Not enough tokens for identifier={}, required={}, available={}", identifier, tokensRequired, updatedTokens);
        // Update storage with current state even if request is denied
        BucketState newState = new BucketState(updatedTokens, currentTime);
        storage.updateBucketState(identifier, newState);
        return new RateLimitResult(false, updatedTokens, capacity, identifier);
    }
    //Consume tokens for the request
    double remainingTokens = updatedTokens - tokensRequired;
    //update storage with new state
    BucketState newState = new BucketState(remainingTokens, currentTime);
    storage.updateBucketState(identifier, newState);

    logger.info("Allowed request for identifier={}, consumed={}, remaining={}", identifier, tokensRequired, remainingTokens);
    return new RateLimitResult(true, remainingTokens, capacity, identifier);
}
@Override
public int getCapacity(){
    return capacity;
}
@Override
public double getRefillRatePerSecond(){
    return refillRatePerSecond;
}
}