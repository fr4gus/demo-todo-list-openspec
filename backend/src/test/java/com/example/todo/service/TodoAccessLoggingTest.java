package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.repository.TodoItemRepository;
import com.example.todo.repository.UserRepository;
import com.example.todo.support.PostgresTestSupport;
import com.example.todo.support.RedisTestSupport;
import com.example.todo.support.TestSecrets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Access to another user's item is logged with ids only; the item's text never reaches the logs. */
@SpringBootTest
@Import({PostgresTestSupport.class, RedisTestSupport.class})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class TodoAccessLoggingTest {

    static final String SECRET_TEXT = "my-very-private-diagnosis";

    @Autowired
    TodoService service;
    @Autowired
    UserRepository users;
    @Autowired
    TodoItemRepository items;

    ListAppender<ILoggingEvent> appender;
    Logger root;
    Level previousLevel;
    User ana;
    User bob;
    UUID anasItem;

    @BeforeEach
    void attach() {
        items.deleteAll();
        users.deleteAll();
        ana = users.save(new User("Ana", "ana@example.com", "hash", Role.USER));
        bob = users.save(new User("Bob", "bob@example.com", "hash", Role.USER));
        anasItem = service.create(ana.getId(), SECRET_TEXT, Priority.HIGH).id();

        root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        previousLevel = root.getLevel();
        root.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void detach() {
        root.detachAppender(appender);
        root.setLevel(previousLevel);
    }

    private List<ILoggingEvent> warnings() {
        return appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
    }

    private List<String> everythingLogged() {
        return appender.list.stream().map(e -> e.getLoggerName() + " " + e.getFormattedMessage() + " "
                + e.getMDCPropertyMap() + " " + e.getThrowableProxy()).toList();
    }

    @Test
    void foreignEditCompleteAndDeleteEachLogAWarningWithBothIdsButNoContent() {
        assertThatThrownBy(() -> service.update(bob.getId(), anasItem, "hacked", Priority.LOW))
                .isInstanceOf(TodoNotFoundException.class);
        assertThatThrownBy(() -> service.setCompletion(bob.getId(), anasItem, true))
                .isInstanceOf(TodoNotFoundException.class);
        assertThatThrownBy(() -> service.delete(bob.getId(), anasItem)).isInstanceOf(TodoNotFoundException.class);

        assertThat(warnings()).hasSize(3).allSatisfy(event -> {
            assertThat(event.getFormattedMessage())
                    .contains(bob.getId().toString())
                    .contains(anasItem.toString())
                    .doesNotContain(SECRET_TEXT);
        });
        assertThat(everythingLogged()).noneMatch(line -> line.contains(SECRET_TEXT));
    }

    @Test
    void anIdThatDoesNotExistAtAllIsNotReportedAsForeignAccess() {
        assertThatThrownBy(() -> service.delete(bob.getId(), UUID.randomUUID())).isInstanceOf(TodoNotFoundException.class);
        assertThatThrownBy(() -> service.update(bob.getId(), UUID.randomUUID(), "x", Priority.LOW))
                .isInstanceOf(TodoNotFoundException.class);

        assertThat(warnings()).isEmpty();
    }

    @Test
    void theOwnersOperationsLogNoWarningAndNoContent() {
        service.update(ana.getId(), anasItem, SECRET_TEXT + "-edited", Priority.LOW);
        service.setCompletion(ana.getId(), anasItem, true);
        service.list(ana.getId());
        service.delete(ana.getId(), anasItem);

        assertThat(warnings()).isEmpty();
        assertThat(everythingLogged()).noneMatch(line -> line.contains(SECRET_TEXT));
    }
}
