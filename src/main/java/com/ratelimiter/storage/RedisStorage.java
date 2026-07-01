package com.ratelimiter.storage;

import com.ratelimiter.core.Storage;
import com.ratelimiter.model.ConsumeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.List;

public class RedisStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(RedisStorage.class);
    private static final String TOKEN_KEY_PREFIX = "bucket:tokens:";
    private static final String TIMESTAMP_KEY_PREFIX = "bucket:ts:";

    /**
     * Refill-and-consume executed atomically inside Redis so concurrent requests
     * (across threads and across server instances) can never observe a stale token
     * count. Keys are set with a PX (millisecond) TTL so idle buckets are reclaimed
     * automatically once they would have fully refilled — bounding Redis memory.
     *
     * KEYS[1] = token key, KEYS[2] = timestamp key
     * ARGV    = capacity, refillRatePerSecond, tokensRequired, nowMillis, ttlMillis
     * returns { allowed (0|1), remainingTokens (string) }
     */
    private static final String TRY_CONSUME_SCRIPT =
        "local tokenKey = KEYS[1]\n" +
        "local tsKey = KEYS[2]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refillRate = tonumber(ARGV[2])\n" +
        "local tokensRequired = tonumber(ARGV[3])\n" +
        "local now = tonumber(ARGV[4])\n" +
        "local ttl = tonumber(ARGV[5])\n" +
        "local tokens = tonumber(redis.call('GET', tokenKey))\n" +
        "local lastTs = tonumber(redis.call('GET', tsKey))\n" +
        "if tokens == nil or lastTs == nil then\n" +
        "  tokens = capacity\n" +
        "  lastTs = now\n" +
        "end\n" +
        "local elapsed = (now - lastTs) / 1000.0\n" +
        "if elapsed < 0 then elapsed = 0 end\n" +
        "tokens = math.min(capacity, tokens + elapsed * refillRate)\n" +
        "local allowed = 0\n" +
        "if tokens >= tokensRequired then\n" +
        "  tokens = tokens - tokensRequired\n" +
        "  allowed = 1\n" +
        "end\n" +
        "redis.call('SET', tokenKey, tokens, 'PX', ttl)\n" +
        "redis.call('SET', tsKey, now, 'PX', ttl)\n" +
        "return { allowed, tostring(tokens) }";

    private final JedisPool jedisPool;

    /** If Redis is unavailable, allow the request (fail open) rather than take the service down. */
    private final boolean failOpen;

    public RedisStorage(String host, int port) {
        this(host, port, true);
    }

    public RedisStorage(String host, int port, boolean failOpen) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);   // MAX CONNECTIONS
        poolConfig.setMaxIdle(32);     // MAX IDLE CONNECTIONS
        poolConfig.setMinIdle(8);      // MIN IDLE CONNECTIONS
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.failOpen = failOpen;
        logger.info("RedisStorage initialized with host={}, port={}, failOpen={}", host, port, failOpen);
    }

    @Override
    public ConsumeResult tryConsume(String identifier, int capacity, double refillRatePerSecond, int tokensRequired) {
        String tokenKey = TOKEN_KEY_PREFIX + identifier;
        String timestampKey = TIMESTAMP_KEY_PREFIX + identifier;
        long now = System.currentTimeMillis();
        // Keep the bucket alive for as long as it could take to refill from empty,
        // plus a small buffer. Once expired the next request just starts full again.
        long ttlMillis = (long) Math.ceil(capacity / refillRatePerSecond * 1000.0) + 1000L;

        try (Jedis jedis = jedisPool.getResource()) {
            Object raw = jedis.eval(
                TRY_CONSUME_SCRIPT,
                Arrays.asList(tokenKey, timestampKey),
                Arrays.asList(
                    String.valueOf(capacity),
                    String.valueOf(refillRatePerSecond),
                    String.valueOf(tokensRequired),
                    String.valueOf(now),
                    String.valueOf(ttlMillis)
                )
            );

            @SuppressWarnings("unchecked")
            List<Object> result = (List<Object>) raw;
            boolean allowed = toLong(result.get(0)) == 1L;
            double remainingTokens = Double.parseDouble(toStr(result.get(1)));
            return new ConsumeResult(allowed, remainingTokens);
        } catch (Exception e) {
            logger.error("Error executing rate-limit script in Redis for identifier: {}", identifier, e);
            if (failOpen) {
                // Fail open: don't let a Redis outage reject all traffic.
                return new ConsumeResult(true, capacity);
            }
            throw new RuntimeException("Failed to evaluate rate limit against Redis", e);
        }
    }

    /** Jedis returns Redis integers as Long. */
    private static long toLong(Object o) {
        if (o instanceof Long) return (Long) o;
        return Long.parseLong(toStr(o));
    }

    /** Bulk strings come back as either String or byte[] depending on the Jedis code path. */
    private static String toStr(Object o) {
        if (o instanceof byte[]) return new String((byte[]) o, java.nio.charset.StandardCharsets.UTF_8);
        return String.valueOf(o);
    }

    @Override
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("RedisStorage closed");
        }
    }
}
