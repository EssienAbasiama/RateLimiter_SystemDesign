# RateLimiter_SystemDesign

<img width="1043" height="538" alt="image" src="https://github.com/user-attachments/assets/b3074472-53e0-478a-9200-9231ac8e967b" />
<img width="1281" height="682" alt="image" src="https://github.com/user-attachments/assets/6df06613-ed72-485a-bafc-2bab0c4459fe" />
<img width="849" height="717" alt="image" src="https://github.com/user-attachments/assets/da9f7418-813b-4135-9587-c6b67141688b" />

```mermaid
sequenceDiagram
    participant Client as HTTP Client
    participant Server as Server Application
    participant Middleware as Rate Limiter Middleware
    participant IDProvider as Request Identifier Provider
    participant RateLimiter as Token Bucket Rate Limiter
    participant Storage as Redis Storage
    participant Redis as Redis Server
    participant API as API Endpoint
    
    Client->>Server: HTTP Request
    Server->>Middleware: Intercept Request
    Middleware->>IDProvider: Extract Identifier
    IDProvider-->>Middleware: identifier (e.g., "ip:192.168.1.1")
    
    Middleware->>RateLimiter: tryConsume(identifier)
    
    Note over RateLimiter: Step 1: Identify user
    RateLimiter->>Storage: getBucketState(identifier)
    Storage->>Redis: GET bucket:tokens:identifier
    Storage->>Redis: GET bucket:ts:identifier
    Redis-->>Storage: tokens, timestamp
    Storage-->>RateLimiter: BucketState
    
    Note over RateLimiter: Step 2-5: Calculate refill
    RateLimiter->>RateLimiter: Calculate elapsed time
    RateLimiter->>RateLimiter: Calculate refill amount
    RateLimiter->>RateLimiter: Update token count
    
    Note over RateLimiter: Step 6: Check tokens
    alt Enough tokens available
        Note over RateLimiter: Step 7: Consume tokens
        RateLimiter->>Storage: updateBucketState(identifier, newState)
        Storage->>Redis: EVAL Lua Script (atomic update)
        Redis-->>Storage: OK
        Storage-->>RateLimiter: Success
        RateLimiter-->>Middleware: RateLimitResult(allowed=true)
        
        Middleware->>API: Forward Request
        API-->>Middleware: Response
        Middleware->>Client: HTTP 200 + Rate Limit Headers
    else Not enough tokens
        RateLimiter-->>Middleware: RateLimitResult(allowed=false)
        Middleware->>Client: HTTP 429 Too Many Requests
    end
```

## Overview

A distributed **token bucket** rate limiter. Each identifier (API key, user id, or IP)
gets a bucket that refills at a fixed rate up to a maximum capacity; a request is
allowed only if it can consume the required tokens.

The refill → check → consume step runs **atomically** in the storage backend, so the
limit holds even under high concurrency and across multiple server instances sharing
one Redis. (With Redis this is a single Lua script; the in-memory backend uses an
atomic `ConcurrentHashMap.compute`.)

## Storage backends

| `storage.type` | Description | Use when |
| -------------- | ----------- | -------- |
| `redis`        | Shared state in Redis, atomic Lua script, keys auto-expire via TTL | Production / multiple instances |
| `memory`       | In-process `ConcurrentHashMap`, no external dependency | Local dev, demos, tests |

## Configuration

Set in `src/main/resources/application.properties` (defaults shown):

```properties
rate.limiter.capacity=10                 # max tokens in the bucket
rate.limiter.refill.rate.per.second=2.0  # tokens added per second
rate.limiter.tokens.required=1           # tokens consumed per request
storage.type=redis                       # redis | memory
redis.host=localhost
redis.port=6379
server.port=8080
```

## Running

```bash
# Server mode (HTTP endpoints behind the limiter)
mvn -q compile exec:java -Dexec.args="--serverside"

# Client mode (interactive rate-limited HTTP client)
mvn -q compile exec:java -Dexec.args="--clientside --userId=user123 --apiKey=key456"
```

Server endpoints: `GET /api/hello`, `GET /api/data`, `POST /api/echo`, `GET /api/health`.
Allowed responses include `X-RateLimit-Remaining` / `X-RateLimit-Capacity` headers;
throttled requests return `429 Too Many Requests`.

## Testing

```bash
mvn test
```

Includes `concurrentRequestsNeverExceedCapacity`, which hammers a single bucket from
50 threads and asserts that **exactly** `capacity` requests are allowed — the guarantee
the limiter exists to provide.
