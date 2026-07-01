package com.ratelimiter.core;

import com.ratelimiter.model.BucketState;
public interface Storage {
    BucketState getBucketState(String identifier);
    void updateBucketState(String identifier, BucketState state);
    void close();
    
}
