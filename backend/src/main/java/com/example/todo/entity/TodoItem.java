package com.example.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "todo_items")
public class TodoItem extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TodoItem() {
    }

    public TodoItem(UUID userId, String text, Priority priority) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.text = text;
        this.priority = priority;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void edit(String text, Priority priority) {
        this.text = text;
        this.priority = priority;
    }

    /** Returns false when the item was already in the requested state, leaving it untouched. */
    public boolean setCompleted(boolean completed, Instant now) {
        if (this.completed == completed) {
            return false;
        }
        this.completed = completed;
        this.completedAt = completed ? now : null;
        return true;
    }

    /** Never include the text: entities get formatted into debug/trace log lines. */
    @Override
    public String toString() {
        return "TodoItem[id=" + id + ", userId=" + userId + "]";
    }
}
