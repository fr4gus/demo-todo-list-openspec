CREATE TABLE todo_items (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text         VARCHAR(500) NOT NULL,
    priority     SMALLINT     NOT NULL,
    completed    BOOLEAN      NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT todo_items_priority_check CHECK (priority BETWEEN 0 AND 3),
    CONSTRAINT todo_items_text_not_empty_check CHECK (char_length(text) > 0)
);

-- Matches the list ordering: completed last, then priority high to low, then oldest first.
CREATE INDEX ix_todo_items_user_order ON todo_items (user_id, completed, priority, created_at);
