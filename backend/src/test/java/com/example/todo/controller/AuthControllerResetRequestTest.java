package com.example.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.config.PropertiesConfig;
import com.example.todo.exception.TooManyRequestsException;
import com.example.todo.security.JwtService;
import com.example.todo.security.ProblemDetailSecurityHandlers;
import com.example.todo.security.SecurityConfig;
import com.example.todo.service.AuthService;
import com.example.todo.service.PasswordResetService;
import com.example.todo.service.RegistrationService;
import com.example.todo.support.ApiSliceTestBeans;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, ApiSliceTestBeans.class, ProblemDetailSecurityHandlers.class,
        PropertiesConfig.class})
@TestPropertySource(properties = {
        "app.jwt-secret=test-secret-test-secret-test-secret-123456",
        "app.frontend-origin=http://frontend.test"
})
class AuthControllerResetRequestTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    PasswordResetService passwordResetService;
    @MockitoBean
    RegistrationService registrationService;
    @MockitoBean
    AuthService authService;

    private org.springframework.test.web.servlet.ResultActions request(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    @Test
    void acceptedWithNeutralMessage() throws Exception {
        request("ana@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If an account exists for that email, a reset link has been sent."));
    }

    @Test
    void rateLimitIs429() throws Exception {
        doThrow(new TooManyRequestsException()).when(passwordResetService).requestReset(any());

        request("ana@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void malformedEmailIs400() throws Exception {
        request("not-an-email").andExpect(status().isBadRequest());
    }
}
