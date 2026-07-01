package com.ratelimiter.provider;

import com.ratelimiter.core.IdentifierProvider;

/**
 * Client-side identifier provider
 * Uses provided userId or API key
 * 
 * Priority: API key > User ID > default
 */
public class ClientIdentifierProvider implements IdentifierProvider {
    private final String identifier;
    public ClientIdentifierProvider(String userId, String apiKey){
        if (apiKey != null && !apiKey.isEmpty()) {
            this.identifier = "apikey:" + apiKey;
        } else if (userId != null && !userId.isEmpty()) {
            this.identifier = "user:" + userId;
        } else {
            this.identifier = "client:default";
        }
    }
    @Override
    public String getIdentifier(){
        return identifier;
    }
    
}
