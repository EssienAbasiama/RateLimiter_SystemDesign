package com.ratelimiter.client;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.IdentifierProvider;
import com.ratelimiter.model.RateLimitResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
public class RateLimitedHttpClient {
    private static final Logger logger=LoggerFactory.getLogger(RateLimitedHttpClient.class);
    private final RateLimiter rateLimiter;
    private final OkHttpClient httpClient;
    private final IdentifierProvider identifierProvider;
    
    public RateLimitedHttpClient(IdentifierProvider identifierProvider, RateLimiter rateLimiter){
        this.identifierProvider = identifierProvider;
        this.rateLimiter = rateLimiter;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    public ClientResponse get(String url) throws IOException, RateLimitExceededException{
        String identifier=identifierProvider.getIdentifier();
        RateLimitResult result=rateLimiter.tryConsume(identifier);
        if (!result.isAllowed()) {
            logger.warn("Rate limit exceeded for client: {}", identifier);
            throw new RateLimitExceededException(
                "Rate limit exceeded. Remaining tokens: " + result.getRemainingTokens(),
                result);
        }
        
        // Make HTTP request
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            return new ClientResponse(
                response.code(),
                response.body() != null ? response.body().string() : "",
                result
            );
        }
    }
    public ClientResponse post(String url, String body) throws IOException, RateLimitExceededException{
        String identifier = identifierProvider.getIdentifier();
        RateLimitResult result = rateLimiter.tryConsume(identifier);
        if (!result.isAllowed()) {
            logger.warn("Rate limit exceeded for client: {}", identifier);
            throw new RateLimitExceededException(
                "Rate limit exceeded. Remaining tokens: " + result.getRemainingTokens(),
                result);
        }
        
        // Make HTTP request
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
            body, okhttp3.MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            return new ClientResponse(
                response.code(),
                response.body() != null ? response.body().string() : "",
                result
            );
        }
    }
    public static class ClientResponse{
        private final int statusCode;
        private final String body;
        private final RateLimitResult rateLimitResult;
        public ClientResponse(int statusCode, String body, RateLimitResult rateLimitResult){
            this.statusCode=statusCode;
            this.body=body;
this.rateLimitResult=rateLimitResult;
        }
        public int getStatusCode(){
            return statusCode;
        }
        public String getBody(){
            return body;
        }
        public RateLimitResult getRateLimitResult(){
            return rateLimitResult;
        }
        @Override
        public String toString(){
            return String.format("ClientResponse{statusCode=%d, body='%s', rateLimitResult=%s}", 
                               statusCode, body, rateLimitResult);
        }
    }
    public static class RateLimitExceededException extends Exception{
        private final RateLimitResult rateLimitResult;
        public RateLimitExceededException(String message, RateLimitResult rateLimitResult){
            super(message);
            this.rateLimitResult=rateLimitResult;
        }
        public RateLimitResult getRateLimitResult(){
            return rateLimitResult;
        }
    }
}

    

