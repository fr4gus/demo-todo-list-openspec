package com.example.todo.service;

import com.example.todo.config.CacheNames;
import com.example.todo.dto.TodoResponse;
import com.example.todo.entity.Priority;
import com.example.todo.entity.TodoItem;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.repository.TodoItemRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every operation takes the caller's id and can only see or change that user's items. The ordered list is cached per
 * user and evicted by every change; the cache advice wraps the transaction (see {@code CacheConfig}), so eviction
 * happens after commit.
 */
@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);

    private final TodoItemRepository items;
    private final Clock clock;

    public TodoService(TodoItemRepository items, Clock clock) {
        this.items = items;
        this.clock = clock;
    }

    @Cacheable(cacheNames = CacheNames.TODOS, key = "#userId")
    @Transactional(readOnly = true)
    public List<TodoResponse> list(UUID userId) {
        return items.findAllOrdered(userId).stream().map(TodoResponse::from).toList();
    }

    @CacheEvict(cacheNames = CacheNames.TODOS, key = "#userId")
    @Transactional
    public TodoResponse create(UUID userId, String text, Priority priority) {
        return TodoResponse.from(items.saveAndFlush(new TodoItem(userId, text.strip(), priority)));
    }

    @CacheEvict(cacheNames = CacheNames.TODOS, key = "#userId")
    @Transactional
    public TodoResponse update(UUID userId, UUID id, String text, Priority priority) {
        TodoItem item = find(userId, id);
        item.edit(text.strip(), priority);
        return TodoResponse.from(items.saveAndFlush(item));
    }

    /** Idempotent: asking for the state the item is already in changes nothing. */
    @CacheEvict(cacheNames = CacheNames.TODOS, key = "#userId")
    @Transactional
    public TodoResponse setCompletion(UUID userId, UUID id, boolean completed) {
        TodoItem item = find(userId, id);
        if (item.setCompleted(completed, clock.instant())) {
            item = items.saveAndFlush(item);
        }
        return TodoResponse.from(item);
    }

    @CacheEvict(cacheNames = CacheNames.TODOS, key = "#userId")
    @Transactional
    public void delete(UUID userId, UUID id) {
        if (items.deleteByIdAndUserId(id, userId) == 0) {
            throw notFound(userId, id);
        }
    }

    private TodoItem find(UUID userId, UUID id) {
        return items.findByIdAndUserId(id, userId).orElseThrow(() -> notFound(userId, id));
    }

    /**
     * Foreign ids answer 404 like missing ones, which hides the attempt from the client; the log is where it stays
     * visible. Only ids are logged, never the item's content.
     */
    private TodoNotFoundException notFound(UUID userId, UUID id) {
        if (items.existsById(id)) {
            log.warn("User {} attempted to access TODO item {} owned by another user", userId, id);
        }
        return new TodoNotFoundException();
    }
}
