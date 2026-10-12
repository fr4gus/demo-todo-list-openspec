package com.example.todo.dto;

import com.example.todo.entity.Priority;
import com.example.todo.entity.TodoItem;
import java.time.Instant;
import java.util.UUID;

public record TodoResponse(UUID id, String text, Priority priority, boolean completed, Instant createdAt) {

    public static TodoResponse from(TodoItem item) {
        return new TodoResponse(item.getId(), item.getText(), item.getPriority(), item.isCompleted(), item.getCreatedAt());
    }
}
