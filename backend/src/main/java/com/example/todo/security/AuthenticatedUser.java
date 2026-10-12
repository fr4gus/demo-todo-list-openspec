package com.example.todo.security;

import com.example.todo.entity.Role;
import java.util.UUID;

/** Principal placed in the SecurityContext for a request with a valid token. */
public record AuthenticatedUser(UUID id, Role role) {
}
