package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todo.dto.TodoResponse;
import com.example.todo.entity.Priority;
import com.example.todo.entity.TodoItem;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.repository.TodoItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Mock
    TodoItemRepository items;

    TodoService service;
    UUID userId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TodoService(items, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void saveReturnsArgument() {
        when(items.saveAndFlush(any(TodoItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listMapsTheRepositoryOrderForTheCaller() {
        TodoItem first = new TodoItem(userId, "first", Priority.HIGH);
        TodoItem second = new TodoItem(userId, "second", Priority.LOW);
        when(items.findAllOrdered(userId)).thenReturn(List.of(first, second));

        List<TodoResponse> result = service.list(userId);

        assertThat(result).extracting(TodoResponse::text).containsExactly("first", "second");
        assertThat(result).extracting(TodoResponse::id).containsExactly(first.getId(), second.getId());
    }

    @Test
    void listIsEmptyWhenTheUserHasNoItems() {
        when(items.findAllOrdered(userId)).thenReturn(List.of());

        assertThat(service.list(userId)).isEmpty();
    }

    @Test
    void createTrimsTextAndOwnsTheItemByTheCaller() {
        saveReturnsArgument();

        TodoResponse created = service.create(userId, "  Buy milk \n", Priority.MEDIUM_HIGH);

        ArgumentCaptor<TodoItem> saved = ArgumentCaptor.forClass(TodoItem.class);
        verify(items).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getText()).isEqualTo("Buy milk");
        assertThat(created.text()).isEqualTo("Buy milk");
        assertThat(created.priority()).isEqualTo(Priority.MEDIUM_HIGH);
        assertThat(created.completed()).isFalse();
    }

    @Test
    void updateChangesTextAndPriorityOnly() {
        TodoItem item = new TodoItem(userId, "old", Priority.LOW);
        item.setCompleted(true, NOW);
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));
        saveReturnsArgument();

        TodoResponse updated = service.update(userId, itemId, " new ", Priority.HIGH);

        assertThat(updated.text()).isEqualTo("new");
        assertThat(updated.priority()).isEqualTo(Priority.HIGH);
        assertThat(updated.completed()).isTrue();
        assertThat(item.getUserId()).isEqualTo(userId);
    }

    @Test
    void updateOfAnItemTheCallerDoesNotOwnIsNotFound() {
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, itemId, "x", Priority.LOW))
                .isInstanceOf(TodoNotFoundException.class);
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void completingMarksTheItemAndRecordsTheTime() {
        TodoItem item = new TodoItem(userId, "x", Priority.LOW);
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));
        saveReturnsArgument();

        TodoResponse result = service.setCompletion(userId, itemId, true);

        assertThat(result.completed()).isTrue();
        assertThat(item.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void restoringClearsTheCompletionTime() {
        TodoItem item = new TodoItem(userId, "x", Priority.LOW);
        item.setCompleted(true, NOW.minusSeconds(60));
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));
        saveReturnsArgument();

        TodoResponse result = service.setCompletion(userId, itemId, false);

        assertThat(result.completed()).isFalse();
        assertThat(item.getCompletedAt()).isNull();
    }

    @Test
    void repeatingTheCurrentStateChangesNothing() {
        TodoItem item = new TodoItem(userId, "x", Priority.LOW);
        item.setCompleted(true, NOW.minusSeconds(60));
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.of(item));

        TodoResponse result = service.setCompletion(userId, itemId, true);

        assertThat(result.completed()).isTrue();
        assertThat(item.getCompletedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(items, never()).saveAndFlush(any());
    }

    @Test
    void completionOfAForeignItemIsNotFound() {
        when(items.findByIdAndUserId(itemId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setCompletion(userId, itemId, true))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void deleteRemovesTheCallersItem() {
        when(items.deleteByIdAndUserId(itemId, userId)).thenReturn(1);

        service.delete(userId, itemId);

        verify(items).deleteByIdAndUserId(itemId, userId);
    }

    @Test
    void deleteOfAForeignOrMissingItemIsNotFound() {
        when(items.deleteByIdAndUserId(itemId, userId)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(userId, itemId)).isInstanceOf(TodoNotFoundException.class);
    }
}
