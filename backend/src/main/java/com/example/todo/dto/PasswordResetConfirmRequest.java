package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "must not be blank") String token,
        @NotBlank(message = "must not be blank") @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String newPassword) {

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[token=***, newPassword=***]";
    }
}
