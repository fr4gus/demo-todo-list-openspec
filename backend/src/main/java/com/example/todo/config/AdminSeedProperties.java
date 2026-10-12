package com.example.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** First administrator, from ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_FULL_NAME. All-or-nothing; see AdminSeeder. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(String email, String password, String fullName) {
}
