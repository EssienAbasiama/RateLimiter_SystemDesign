package com.ratelimiter.provider;

import com.ratelimiter.core.IdentifierProvider;
import spark.Request;
/**
 * Server-side identifier provider
 * Extracts identifier from HTTP request
 * 
 * Priority order:
 * 1. API-Key header (if present)
 * 2. User-Id header (if present)
 * 3. IP address (fallback)
 */

public class RequestIdentifierProvider implements IdentifierProvider{
    private final Request request;
    public RequestIdentifierProvider(Request request){
        if(request==null)throw new IllegalArgumentException("Request cannot be null");
        this.request=request;
    }
    @Override
    public String getIdentifier(){
               // Priority: API-Key header > User-Id header > IP address
               String apiKey = request.headers("API-Key");
               if (apiKey != null && !apiKey.isEmpty()) {
                   return "apikey:" + apiKey;
               }
               
               String userId = request.headers("User-Id");
               if (userId != null && !userId.isEmpty()) {
                   return "user:" + userId;
               }
               
               String ip = request.ip();
               return "ip:" + (ip != null ? ip : "unknown");
           }
}
