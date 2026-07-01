package com.ratelimiter.storage;

import com.ratelimiter.core.Storage;
import com.ratelimiter.model.BucketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisStorage implements Storage{
    private static final Logger logger = LoggerFactory.getLogger(RedisStorage.class);
    private static final String TOKEN_KEY_PREFIX = "bucket:tokens:";
    private static final String TIMESTAMP_KEY_PREFIX = "bucket:ts:";

    private final JedisPool jedisPool;
    private static final String UPDATE_BUCKET_SCRIPT = 
        "local tokenKey = KEYS[1]\n" +
        "local tsKey = KEYS[2]\n" +
        "local tokens = tonumber(ARGV[1])\n" +
        "local timestamp = tonumber(ARGV[2])\n" +
        "redis.call('SET', tokenKey, tokens)\n" +
        "redis.call('SET', tsKey, timestamp)\n" +
        "return {tokens, timestamp}";

    public RedisStorage(String host, int port){
        JedisPoolConfig poolConfig=new JedisPoolConfig();
        poolConfig.setMaxTotal(128);//MAX CONNECTIONS
        poolConfig.setMaxIdle(32);//MAX IDLE CONNECTIONS
        poolConfig.setMinIdle(8);//MIN IDLE CONNECTIONS
        poolConfig.setTestOnBorrow(true);//TEST CONNECTIONS ON BEFORE USE
        poolConfig.setTestOnReturn(true);//TEST CONNECTIONS ON RETURN
        this.jedisPool=new JedisPool(poolConfig, host,port);
        logger.info("RedisStorage initialized with host={}, port={}", host, port);
    }
    @Override
    public BucketState getBucketState(String identifier){
        String tokenKey=TOKEN_KEY_PREFIX+identifier;
        String timestampKey=TIMESTAMP_KEY_PREFIX+identifier;

        try(Jedis jedis=jedisPool.getResource()){
            String tokenValue=jedis.get(tokenKey);
            String timestampValue=jedis.get(timestampKey);
            if(tokenValue==null||timestampValue==null){
                logger.info("No bucket state found for identifier: {}", identifier);
                return null;
            }
            double tokens=Double.parseDouble(tokenValue);
            long timestamp=Long.parseLong(timestampValue);

            return new BucketState(tokens, timestamp);
        }catch(Exception e){
            logger.error("Error getting bucket state from Redis for identifier: {}", identifier, e);
            return null;
        }
    }
    
    @Override
    public void updateBucketState(String identifier, BucketState state){
        String tokenKey = TOKEN_KEY_PREFIX + identifier;
        String timestampKey = TIMESTAMP_KEY_PREFIX + identifier;
        
        try(Jedis jedis = jedisPool.getResource()){
            jedis.set(tokenKey, String.valueOf(state.getTokens()));
            jedis.set(timestampKey, String.valueOf(state.getLastRefillTimestamp()));
            logger.debug("Updated bucket state for identifier: {}", identifier);
        } catch(Exception e){
            logger.error("Error updating bucket state in Redis for identifier: {}", identifier, e);
            throw new RuntimeException("Failed to update bucket state", e);
        }
    }
    
    @Override
    public void close(){
        if(jedisPool != null && !jedisPool.isClosed()){
            jedisPool.close();
            logger.info("RedisStorage closed");
        }
    }

}
