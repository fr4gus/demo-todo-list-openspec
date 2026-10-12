package com.example.todo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionRequest(@NotNull(message = "must be true or false") Boolean completed) {
}
