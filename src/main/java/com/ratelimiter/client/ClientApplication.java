package com.ratelimiter.client;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimiterConfig;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.provider.ClientIdentifierProvider;
import com.ratelimiter.core.Storage;
import com.ratelimiter.storage.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
public class ClientApplication {
    private static final Logger logger=LoggerFactory.getLogger(ClientApplication.class);
    private final RateLimiterConfig config;
    private final Storage storage;
    private final RateLimiter rateLimiter;
    private final RateLimitedHttpClient client;

    public ClientApplication(RateLimiterConfig config, String userId, String apiKey){
        this.config=config;
        this.storage=StorageFactory.createStorage(config);
        this.rateLimiter=new TokenBucketRateLimiter(
            storage,
            config.getCapacity(),
            config.getRefillRatePerSecond(),
            config.getTokensRequired()
        );
        ClientIdentifierProvider provider=new ClientIdentifierProvider(userId,apiKey);
        this.client=new RateLimitedHttpClient(provider,rateLimiter);
    }
    public void run(){
        Scanner scanner=new Scanner(System.in);
        logger.info("Client application started");
        logger.info("Rate limiter configured: capacity={}, refillRate={}/sec", 
                   config.getCapacity(), config.getRefillRatePerSecond());
        logger.info("Enter URL to make requests (or 'quit' to exit):");
        while (true) {
            System.out.print("URL: ");
            String url = scanner.nextLine();
            
            if ("quit".equalsIgnoreCase(url) || "exit".equalsIgnoreCase(url)) {
                break;
            }
            
            try {
                RateLimitedHttpClient.ClientResponse response = client.get(url);
                System.out.println("Status: " + response.getStatusCode());
                System.out.println("Response: " + response.getBody());
                System.out.println("Remaining tokens: " + 
                                 response.getRateLimitResult().getRemainingTokens());
            } catch (RateLimitedHttpClient.RateLimitExceededException e) {
                System.out.println("ERROR: " + e.getMessage());
                System.out.println("Capacity: " + e.getRateLimitResult().getCapacity());
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                logger.error("Error making request", e);
            }
        }
        scanner.close();
        storage.close();
        logger.info("Client application stopped");
    }
}
