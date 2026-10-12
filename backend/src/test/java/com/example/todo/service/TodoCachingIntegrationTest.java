package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.dto.TodoResponse;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.TodoItemRepository;
import com.example.todo.repository.UserRepository;
import com.example.todo.support.PostgresTestSupport;
import com.example.todo.support.TestSecrets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real PostgreSQL and Redis: every change must be visible in the very next list, and caches stay per user. */
@SpringBootTest
@Import(PostgresTestSupport.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class TodoCachingIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    TodoService service;
    @Autowired
    UserRepository users;
    @Autowired
    TodoItemRepository items;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    JdbcTemplate jdbc;

    User ana;
    User bob;

    @BeforeEach
    void seed() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        items.deleteAll();
        users.deleteAll();
        ana = users.save(new User("Ana", "ana@example.com", "hash", Role.USER));
        bob = users.save(new User("Bob", "bob@example.com", "hash", Role.USER));
    }

    private List<String> texts(User user) {
        return service.list(user.getId()).stream().map(TodoResponse::text).toList();
    }

    private String cacheKey(User user) {
        return "todos::" + user.getId();
    }

    @Test
    void listIsServedFromRedisUntilSomethingChanges() {
        service.create(ana.getId(), "one", Priority.HIGH);
        assertThat(texts(ana)).containsExactly("one");
        assertThat(redisTemplate.hasKey(cacheKey(ana))).isTrue();

        // A change made behind the service's back is not seen while the entry is cached.
        jdbc.update("UPDATE todo_items SET text = 'sneaky'");
        assertThat(texts(ana)).containsExactly("one");
    }

    @Test
    void listIsReadableAgainAfterRoundTripThroughRedis() {
        service.create(ana.getId(), "one", Priority.LOW);
        List<TodoResponse> fromDatabase = service.list(ana.getId());

        List<TodoResponse> fromRedis = service.list(ana.getId());

        assertThat(redisTemplate.opsForValue().get(cacheKey(ana))).contains("\"text\":\"one\"");
        assertThat(fromRedis).isEqualTo(fromDatabase);
        assertThat(fromRedis.get(0).createdAt()).isNotNull();
    }

    @Test
    void createIsVisibleInTheNextList() {
        assertThat(texts(ana)).isEmpty();

        service.create(ana.getId(), "new", Priority.HIGH);

        assertThat(texts(ana)).containsExactly("new");
    }

    @Test
    void updateIsVisibleInTheNextList() {
        UUID id = service.create(ana.getId(), "old", Priority.HIGH).id();
        texts(ana);

        service.update(ana.getId(), id, "changed", Priority.LOW);

        assertThat(service.list(ana.getId())).singleElement().satisfies(t -> {
            assertThat(t.text()).isEqualTo("changed");
            assertThat(t.priority()).isEqualTo(Priority.LOW);
        });
    }

    @Test
    void completionAndRestoreAreVisibleInTheNextList() {
        UUID first = service.create(ana.getId(), "first", Priority.LOW).id();
        service.create(ana.getId(), "second", Priority.HIGH);
        assertThat(texts(ana)).containsExactly("second", "first");

        service.setCompletion(ana.getId(), first, true);
        assertThat(service.list(ana.getId())).extracting(TodoResponse::completed).containsExactly(false, true);

        service.setCompletion(ana.getId(), first, false);
        assertThat(service.list(ana.getId())).extracting(TodoResponse::completed).containsExactly(false, false);
    }

    @Test
    void deleteIsVisibleInTheNextList() {
        UUID id = service.create(ana.getId(), "gone", Priority.HIGH).id();
        assertThat(texts(ana)).containsExactly("gone");

        service.delete(ana.getId(), id);

        assertThat(texts(ana)).isEmpty();
    }

    @Test
    void oneUsersChangesDoNotTouchAnotherUsersCache() {
        service.create(ana.getId(), "ana-item", Priority.HIGH);
        texts(ana);
        assertThat(redisTemplate.hasKey(cacheKey(ana))).isTrue();

        service.create(bob.getId(), "bob-item", Priority.HIGH);

        assertThat(redisTemplate.hasKey(cacheKey(ana))).isTrue();
        assertThat(texts(ana)).containsExactly("ana-item");
        assertThat(texts(bob)).containsExactly("bob-item");
    }
}
