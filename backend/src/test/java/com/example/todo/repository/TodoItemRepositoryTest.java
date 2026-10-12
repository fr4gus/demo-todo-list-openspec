package com.example.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.config.JpaConfig;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Role;
import com.example.todo.entity.TodoItem;
import com.example.todo.entity.User;
import com.example.todo.support.PostgresTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestSupport.class, JpaConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class TodoItemRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    UserRepository users;
    @Autowired
    TodoItemRepository items;
    @Autowired
    JdbcTemplate jdbc;

    private User newUser(String email) {
        return users.saveAndFlush(new User("Ana", email, "hash", Role.USER));
    }

    /** Saves an item and pins its creation time so ordering assertions do not depend on the clock. */
    private TodoItem item(User owner, String text, Priority priority, boolean completed, int minutesAfterStart) {
        TodoItem item = new TodoItem(owner.getId(), text, priority);
        if (completed) {
            item.setCompleted(true, T0);
        }
        items.saveAndFlush(item);
        jdbc.update("UPDATE todo_items SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(T0.plusSeconds(60L * minutesAfterStart)), item.getId());
        return item;
    }

    private List<String> orderedTexts(User owner) {
        items.flush();
        return items.findAllOrdered(owner.getId()).stream().map(TodoItem::getText).toList();
    }

    @Test
    void persistsAllFieldsAndAuditTimes() {
        User ana = newUser("a@example.com");
        TodoItem saved = items.saveAndFlush(new TodoItem(ana.getId(), "Buy milk", Priority.MEDIUM_LOW));

        TodoItem found = items.findByIdAndUserId(saved.getId(), ana.getId()).orElseThrow();
        assertThat(found.getText()).isEqualTo("Buy milk");
        assertThat(found.getPriority()).isEqualTo(Priority.MEDIUM_LOW);
        assertThat(found.isCompleted()).isFalse();
        assertThat(found.getCompletedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT priority FROM todo_items WHERE id = ?", Integer.class, saved.getId()))
                .isEqualTo(2);
    }

    @Test
    void ordersActiveBeforeCompletedThenByPriorityThenOldestFirst() {
        User ana = newUser("a@example.com");
        item(ana, "completed-high", Priority.HIGH, true, 0);
        item(ana, "active-low-old", Priority.LOW, false, 1);
        item(ana, "active-medium-low", Priority.MEDIUM_LOW, false, 2);
        item(ana, "active-high-new", Priority.HIGH, false, 9);
        item(ana, "active-medium-high", Priority.MEDIUM_HIGH, false, 3);
        item(ana, "completed-low", Priority.LOW, true, 4);
        item(ana, "active-high-old", Priority.HIGH, false, 5);

        assertThat(orderedTexts(ana)).containsExactly(
                "active-high-old", "active-high-new", "active-medium-high", "active-medium-low", "active-low-old",
                "completed-high", "completed-low");
    }

    @Test
    void itemsCreatedAtTheSameInstantKeepAStableOrder() {
        User ana = newUser("a@example.com");
        for (int i = 0; i < 5; i++) {
            item(ana, "same-" + i, Priority.HIGH, false, 0);
        }

        assertThat(orderedTexts(ana)).isEqualTo(orderedTexts(ana)).hasSize(5);
    }

    @Test
    void listingAndLookupAreScopedToTheOwner() {
        User ana = newUser("a@example.com");
        User bob = newUser("b@example.com");
        TodoItem anas = item(ana, "ana-item", Priority.HIGH, false, 0);
        item(bob, "bob-item", Priority.HIGH, false, 0);

        assertThat(orderedTexts(ana)).containsExactly("ana-item");
        assertThat(orderedTexts(bob)).containsExactly("bob-item");
        assertThat(items.findByIdAndUserId(anas.getId(), ana.getId())).isPresent();
        assertThat(items.findByIdAndUserId(anas.getId(), bob.getId())).isEmpty();
    }

    @Test
    void deleteOnlyRemovesTheOwnersItem() {
        User ana = newUser("a@example.com");
        User bob = newUser("b@example.com");
        TodoItem anas = item(ana, "ana-item", Priority.HIGH, false, 0);

        assertThat(items.deleteByIdAndUserId(anas.getId(), bob.getId())).isZero();
        assertThat(orderedTexts(ana)).containsExactly("ana-item");

        assertThat(items.deleteByIdAndUserId(anas.getId(), ana.getId())).isEqualTo(1);
        assertThat(orderedTexts(ana)).isEmpty();
    }

    @Test
    void completionTimeIsStoredAndClearedOnRestore() {
        User ana = newUser("a@example.com");
        TodoItem saved = item(ana, "x", Priority.LOW, false, 0);

        saved.setCompleted(true, T0);
        items.saveAndFlush(saved);
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM todo_items WHERE id = ?", Boolean.class, saved.getId())).isTrue();

        saved.setCompleted(false, T0);
        items.saveAndFlush(saved);
        assertThat(jdbc.queryForObject("SELECT completed_at IS NULL FROM todo_items WHERE id = ?", Boolean.class, saved.getId())).isTrue();
    }

    @Test
    void itemMustReferenceAnExistingUser() {
        assertThatThrownBy(() -> items.saveAndFlush(new TodoItem(UUID.randomUUID(), "orphan", Priority.LOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsPriorityOutsideTheEnumRange() {
        User ana = newUser("a@example.com");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO todo_items (id, user_id, text, priority) VALUES (?, ?, 'x', 4)", UUID.randomUUID(), ana.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsEmptyText() {
        User ana = newUser("a@example.com");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO todo_items (id, user_id, text, priority) VALUES (?, ?, '', 0)", UUID.randomUUID(), ana.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheUserCascadesToTheirItems() {
        User ana = newUser("a@example.com");
        User bob = newUser("b@example.com");
        item(ana, "a1", Priority.HIGH, false, 0);
        item(ana, "a2", Priority.LOW, true, 1);
        item(bob, "b1", Priority.HIGH, false, 0);

        jdbc.update("DELETE FROM users WHERE id = ?", ana.getId());

        assertThat(jdbc.queryForList("SELECT text FROM todo_items", String.class)).containsExactly("b1");
    }
}
