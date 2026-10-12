package com.example.todo.support;

import com.example.todo.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that trigger each error path. */
@RestController
@RequestMapping("/api/v1/_test")
public class SampleErrorController {

    public record Payload(@NotBlank(message = "must not be blank") String name) {
    }

    @PostMapping("/validate")
    public void validate(@Valid @RequestBody Payload payload) {
    }

    @GetMapping("/boom")
    public void boom() {
        throw new IllegalStateException("secret internal detail com.example.todo.Internal");
    }

    @GetMapping("/api-error")
    public void apiError() {
        throw new ApiException(HttpStatus.CONFLICT, "SAMPLE_CONFLICT", "Sample conflict.");
    }

    @GetMapping("/me")
    public String me() {
        return com.example.todo.security.CurrentUser.id().toString();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public void admin() {
    }
}
