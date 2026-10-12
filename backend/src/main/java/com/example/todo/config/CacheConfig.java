package com.example.todo.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** Redis-backed caching. Feature changes declare their cache names in {@link CacheNames}. */
@Configuration
@EnableCaching(order = CacheConfig.ADVICE_ORDER)
public class CacheConfig {

    /** One step outside the transaction advice (lowest precedence), so evictions run after the commit. */
    public static final int ADVICE_ORDER = Ordered.LOWEST_PRECEDENCE - 1;

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
