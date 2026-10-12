package com.example.todo.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank(message = "JWT_SECRET environment variable is required") String jwtSecret,
        @NotBlank(message = "FRONTEND_ORIGIN environment variable is required") String frontendOrigin) {
}
