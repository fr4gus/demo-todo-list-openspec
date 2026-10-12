package com.example.todo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Unknown properties (such as {@code role}) are ignored, so a role can never be chosen by the caller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
        @NotBlank(message = "must not be blank") @Size(max = 255, message = "must be at most 255 characters") String fullName,
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") @Size(max = 320, message = "must be at most 320 characters") String email,
        @NotBlank(message = "must not be blank") @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password) {

    /** Records print every component by default; keep the password out of any log line that formats the request. */
    @Override
    public String toString() {
        return "RegisterRequest[fullName=" + fullName + ", email=" + email + ", password=***]";
    }
}
