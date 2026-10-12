package com.example.todo.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Real Redis shared across the test JVM, pointed at by the application's Redis properties. */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestSupport {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @Bean
    DynamicPropertyRegistrar redisProperties() {
        return registry -> {
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        };
    }
}
