package com.example.todo.service;

/** Published inside the request transaction; the mail is sent only after it commits. */
public record PasswordResetRequested(String email, String fullName, String token) {

    @Override
    public String toString() {
        return "PasswordResetRequested[email=" + email + ", token=***]";
    }
}
