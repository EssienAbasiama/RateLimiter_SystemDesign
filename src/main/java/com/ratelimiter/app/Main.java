package com.ratelimiter.app;

import com.ratelimiter.client.ClientApplication;
import com.ratelimiter.config.RateLimiterConfig;
import com.ratelimiter.server.ServerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// *   Server-side: java Main --serverside
// *   Client-side: java Main --clientside

public class Main {
    private static final Logger logger=LoggerFactory.getLogger(Main.class);
    public static void main(String[] args){
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        String mode=args[0].toLowerCase();
        RateLimiterConfig config = new RateLimiterConfig();
        try {
            if ("--serverside".equals(mode) || "--server".equals(mode)) {
                runServerSide(config);
            } else if ("--clientside".equals(mode) || "--client".equals(mode)) {
                runClientSide(config, args);
            } else {
                System.err.println("Unknown mode: " + mode);
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            logger.error("Application error", e);
            System.exit(1);
        }


    }
    private static void runServerSide(RateLimiterConfig config){
        logger.info("Starting SERVER-SIDE rate limiter application");
        ServerApplication server = new ServerApplication(config);
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            server.stop();
        }));
        
        server.start();
    }
    private static void runClientSide(RateLimiterConfig config, String[] args){
        String userId = null;
        String apiKey = null;
        
        // Parse command line arguments
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--userId=")) {
                userId = arg.substring("--userId=".length());
            } else if (arg.startsWith("--apiKey=")) {
                apiKey = arg.substring("--apiKey=".length());
            }
        }
        
        if (userId == null || userId.isEmpty()) {
            userId = "default-user";
            logger.warn("No userId provided, using default: {}", userId);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "default-key";
            logger.warn("No apiKey provided, using default: {}", apiKey);
        }
        
        logger.info("Starting CLIENT-SIDE rate limiter application");
        ClientApplication client = new ClientApplication(config, userId, apiKey);
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down client...");
            // Client cleanup if needed
        }));
        
        client.run();
    }
    private static void printUsage(){
        System.out.println("Rate Limiter Application");
        System.out.println("Usage:");
        System.out.println("  Server-side: java Main --serverside");
        System.out.println("  Client-side: java Main --clientside [--userId=USER_ID] [--apiKey=API_KEY]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main --serverside");
        System.out.println("  java Main --clientside --userId=user123 --apiKey=key456");
    }
}


