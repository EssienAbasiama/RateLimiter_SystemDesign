package com.ratelimiter.storage;

import com.ratelimiter.core.Storage;
import com.ratelimiter.config.RateLimiterConfig;

public class StorageFactory {
    public static Storage createStorage(
RateLimiterConfig config        
    ){
        String storageType=config.getStorageType();
        switch(storageType.toLowerCase()){
            case "redis":
                return new RedisStorage(config.getRedisHost(), config.getRedisPort());
            default:
                throw new IllegalArgumentException("Invalid storage type: "+storageType);
        }

    }
}