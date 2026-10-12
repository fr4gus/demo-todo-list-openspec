package com.example.testsupport.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.config.CacheConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = CacheConfigIntegrationTest.TestApp.class)
@Testcontainers(disabledWithoutDocker = true)
class CacheConfigIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    record Item(String id, String label) {
    }

    @Configuration
    @ImportAutoConfiguration({RedisAutoConfiguration.class, CacheAutoConfiguration.class})
    static class TestApp {
        @Bean
        CacheConfig cacheConfig() {
            return new CacheConfig();
        }

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }

    static class SampleService {
        final AtomicInteger loads = new AtomicInteger();

        @Cacheable(cacheNames = "sample", key = "#id")
        public Item load(String id) {
            loads.incrementAndGet();
            return new Item(id, "label-" + id);
        }

        @CacheEvict(cacheNames = "sample", key = "#id")
        public void evict(String id) {
        }
    }

    @Autowired
    SampleService service;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void flush() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        service.loads.set(0);
    }

    @Test
    void storesAndEvictsEntryWithJsonValueAndTtl() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        Item first = service.load("1");
        Item second = service.load("1");
        assertThat(service.loads).hasValue(1);
        assertThat(second).isEqualTo(first);

        String key = "sample::1";
        assertThat(redisTemplate.opsForValue().get(key)).contains("label-1");
        Long ttl = redisTemplate.getExpire(key);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());

        service.evict("1");
        assertThat(redisTemplate.hasKey(key)).isFalse();
        service.load("1");
        assertThat(service.loads).hasValue(2);
    }
}
