package com.example.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.config.PropertiesConfig;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.UserRepository;
import com.example.todo.security.JwtService;
import com.example.todo.security.ProblemDetailSecurityHandlers;
import com.example.todo.security.SecurityConfig;
import com.example.todo.service.AuthService;
import com.example.todo.service.PasswordResetService;
import com.example.todo.service.RegistrationService;
import com.example.todo.support.ApiSliceTestBeans;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Login through the real AuthService and JwtService; only persistence is stubbed. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, AuthService.class, ApiSliceTestBeans.class,
        ProblemDetailSecurityHandlers.class, PropertiesConfig.class})
@TestPropertySource(properties = {
        "app.jwt-secret=test-secret-test-secret-test-secret-123456",
        "app.frontend-origin=http://frontend.test"
})
class AuthControllerLoginTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    RegistrationService registrationService;
    @MockitoBean
    PasswordResetService passwordResetService;

    User ana;

    @BeforeEach
    void setUp() {
        ana = new User("Ana Perez", "ana@example.com", new BCryptPasswordEncoder(4).encode("s3cretpass"), Role.USER);
        org.mockito.Mockito.when(users.findByEmail("ana@example.com")).thenReturn(java.util.Optional.of(ana));
        org.mockito.Mockito.when(users.findByEmail("nobody@example.com")).thenReturn(java.util.Optional.empty());
    }

    private String body(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    @Test
    void successReturnsTokenExpiringIn24HoursAndProfile() throws Exception {
        Instant before = Instant.now();
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana@Example.com", "s3cretpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(ana.getId().toString()))
                .andExpect(jsonPath("$.user.fullName").value("Ana Perez"))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(json, "$.token");
        Instant expiresAt = Instant.parse(com.jayway.jsonpath.JsonPath.read(json, "$.expiresAt"));
        assertThat(jwtService.parse(token).userId()).isEqualTo(ana.getId());
        assertThat(Duration.between(before, expiresAt)).isBetween(Duration.ofHours(24).minusSeconds(5), Duration.ofHours(24).plusSeconds(5));
    }

    @Test
    void unknownEmailIs401WithExactMessage() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("nobody@example.com", "whatever1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Email doesn't exist"));
    }

    @Test
    void wrongPasswordIs401WithExactMessage() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.detail").value("invalid password"));
    }

    @Test
    void blankFieldsAre400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
