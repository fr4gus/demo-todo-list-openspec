package com.example.todo.dto;

import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import java.time.Instant;
import java.util.UUID;

public record LoginResponse(String token, Instant expiresAt, UserProfile user) {

    /** The token is a credential; response bodies get formatted into debug/trace log lines. */
    @Override
    public String toString() {
        return "LoginResponse[token=***, expiresAt=" + expiresAt + ", user=" + user + "]";
    }

    public record UserProfile(UUID id, String fullName, String email, Role role) {

        public static UserProfile from(User user) {
            return new UserProfile(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
        }
    }
}
