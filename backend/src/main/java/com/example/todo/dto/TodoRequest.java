package com.example.todo.dto;

import com.example.todo.entity.Priority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for create and edit. The owner is never part of the body; it comes from the token. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TodoRequest(
        @NotBlank(message = "must not be blank") @Size(max = 500, message = "must be at most 500 characters") String text,
        @NotNull(message = "must be one of HIGH, MEDIUM_HIGH, MEDIUM_LOW, LOW") Priority priority) {

    /** The limit applies to the trimmed text, so surrounding whitespace is dropped before validation runs. */
    public TodoRequest {
        text = text == null ? null : text.strip();
    }
}
