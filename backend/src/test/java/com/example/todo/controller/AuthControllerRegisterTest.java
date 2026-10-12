package com.example.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.config.PropertiesConfig;
import com.example.todo.dto.RegisterRequest;
import com.example.todo.exception.EmailInUseException;
import com.example.todo.security.ProblemDetailSecurityHandlers;
import com.example.todo.security.JwtService;
import com.example.todo.security.SecurityConfig;
import com.example.todo.support.ApiSliceTestBeans;
import com.example.todo.service.AuthService;
import com.example.todo.service.PasswordResetService;
import com.example.todo.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, ApiSliceTestBeans.class, ProblemDetailSecurityHandlers.class, PropertiesConfig.class})
@TestPropertySource(properties = {
        "app.jwt-secret=test-secret-test-secret-test-secret-123456",
        "app.frontend-origin=http://frontend.test"
})
class AuthControllerRegisterTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    RegistrationService registrationService;
    @MockitoBean
    AuthService authService;
    @MockitoBean
    PasswordResetService passwordResetService;

    private String json(String fullName, String email, String password) {
        return """
                {"fullName":"%s","email":"%s","password":"%s"}""".formatted(fullName, email, password);
    }

    @Test
    void validRegistrationReturns201WithNoBody() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Perez", "ana@example.com", "s3cretpass")))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void roleFieldIsIgnored() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"s3cretpass\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        ArgumentCaptor<RegisterRequest> captor = forClass(RegisterRequest.class);
        verify(registrationService).register(captor.capture());
        assertThat(captor.getValue().fullName()).isEqualTo("Ana");
    }

    @Test
    void invalidInputReturns400ListingFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(" ", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field=='fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());
    }

    @Test
    void duplicateEmailReturns409WithCode() throws Exception {
        when(registrationService.register(any())).thenThrow(new EmailInUseException());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana", "ana@example.com", "s3cretpass")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_IN_USE"));
    }
}
