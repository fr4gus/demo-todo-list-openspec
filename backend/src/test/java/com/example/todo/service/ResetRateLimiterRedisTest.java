package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = ResetRateLimiterRedisTest.TestApp.class)
@Testcontainers(disabledWithoutDocker = true)
class ResetRateLimiterRedisTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Configuration
    @ImportAutoConfiguration(RedisAutoConfiguration.class)
    @Import(ResetRateLimiter.class)
    static class TestApp {
    }

    @Autowired
    ResetRateLimiter limiter;
    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void flush() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void firstFiveRequestsPassAndTheSixthIsRejected() {
        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire("ana@example.com")).as("request %d", i).isTrue();
        }
        assertThat(limiter.tryAcquire("ana@example.com")).isFalse();
        assertThat(limiter.tryAcquire("ana@example.com")).isFalse();
    }

    @Test
    void limitIsPerEmailAndWindowExpiresAfterAnHour() {
        for (int i = 0; i < 6; i++) {
            limiter.tryAcquire("ana@example.com");
        }

        assertThat(limiter.tryAcquire("bob@example.com")).isTrue();
        Long ttl = redisTemplate.getExpire("rate:password-reset:ana@example.com");
        assertThat(ttl).isPositive().isLessThanOrEqualTo(3600L);
    }
}
