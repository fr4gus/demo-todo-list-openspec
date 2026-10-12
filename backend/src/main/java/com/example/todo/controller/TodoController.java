package com.example.todo.controller;

import com.example.todo.dto.CompletionRequest;
import com.example.todo.dto.TodoRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.security.CurrentUser;
import com.example.todo.service.TodoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The owner is always the authenticated caller; no user id appears in any path or body. Administrators get 403. */
@RestController
@RequestMapping("/api/v1/todos")
@PreAuthorize("hasRole('USER')")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoResponse> list() {
        return todoService.list(CurrentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponse create(@Valid @RequestBody TodoRequest request) {
        return todoService.create(CurrentUser.id(), request.text(), request.priority());
    }

    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable UUID id, @Valid @RequestBody TodoRequest request) {
        return todoService.update(CurrentUser.id(), id, request.text(), request.priority());
    }

    @PatchMapping("/{id}/completion")
    public TodoResponse setCompletion(@PathVariable UUID id, @Valid @RequestBody CompletionRequest request) {
        return todoService.setCompletion(CurrentUser.id(), id, request.completed());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        todoService.delete(CurrentUser.id(), id);
    }
}
