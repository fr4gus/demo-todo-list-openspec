package com.example.todo.config;

import com.example.todo.dto.TodoResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * The TODO list is stored as a plain JSON array of {@link TodoResponse}. The platform's generic serializer cannot read
 * a list of final records back (their elements carry no type id), so this cache is given a serializer for the exact type.
 */
@Configuration
public class TodoCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer todoCacheCustomizer(RedisCacheConfiguration defaults) {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, TodoResponse.class);
        RedisCacheConfiguration todos = defaults.serializeValuesWith(
                SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, listType)));
        return builder -> builder.withCacheConfiguration(CacheNames.TODOS, todos);
    }
}
