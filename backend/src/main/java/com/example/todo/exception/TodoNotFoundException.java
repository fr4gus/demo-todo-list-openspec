package com.example.todo.exception;

import org.springframework.http.HttpStatus;

/** Used both for ids that do not exist and for ids owned by someone else, so ids cannot be probed. */
public class TodoNotFoundException extends ApiException {

    public TodoNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO item not found.");
    }
}
