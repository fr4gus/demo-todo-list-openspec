package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.UserRepository;
import com.example.todo.support.TestSecrets;
import com.example.todo.support.PostgresTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The whole recovery flow against real PostgreSQL; Redis rate limiting and SMTP are replaced. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestSupport.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class PasswordResetFlowIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    PasswordResetTokenRepository tokens;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JdbcTemplate jdbc;
    @MockitoBean
    ResetRateLimiter rateLimiter;
    @MockitoBean
    JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        Mockito.reset(mailSender);
        users.deleteAll();
        users.save(new User("Ana", "ana@example.com", encoder.encode("old-password"), Role.USER));
        when(rateLimiter.tryAcquire(any())).thenReturn(true);
    }

    private void requestReset(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))).andExpect(status().isAccepted());
    }

    private List<String> emailedTokens(int expectedMails) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, Mockito.times(expectedMails)).send(captor.capture());
        Pattern p = Pattern.compile("http://frontend\\.test/reset-password\\?token=([A-Za-z0-9_-]+)");
        return captor.getAllValues().stream().map(m -> {
            Matcher matcher = p.matcher(m.getText());
            assertThat(matcher.find()).isTrue();
            return matcher.group(1);
        }).toList();
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, password)));
    }

    @Test
    void requestThenConfirmChangesThePasswordAndTheTokenWorksOnce() throws Exception {
        requestReset("ana@example.com");
        String token = emailedTokens(1).get(0);
        assertThat(jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens", String.class)).isNotEqualTo(token);

        confirm(token, "new-password-1").andExpect(status().isNoContent());

        String hash = users.findByEmail("ana@example.com").orElseThrow().getPasswordHash();
        assertThat(encoder.matches("new-password-1", hash)).isTrue();
        assertThat(encoder.matches("old-password", hash)).isFalse();

        confirm(token, "another-pass-2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void weakPasswordIs400AndLeavesTheTokenUsable() throws Exception {
        requestReset("ana@example.com");
        String token = emailedTokens(1).get(0);

        confirm(token, "short").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        confirm(token, "long-enough-pass").andExpect(status().isNoContent());
    }

    @Test
    void newerRequestSupersedesTheEarlierToken() throws Exception {
        requestReset("ana@example.com");
        requestReset("ana@example.com");
        List<String> mailed = emailedTokens(2);

        confirm(mailed.get(0), "long-enough-pass").andExpect(status().isBadRequest());
        confirm(mailed.get(1), "long-enough-pass").andExpect(status().isNoContent());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        requestReset("ana@example.com");
        String token = emailedTokens(1).get(0);
        jdbc.update("UPDATE password_reset_tokens SET expires_at = ?", java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(1))));

        confirm(token, "long-enough-pass").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void unknownEmailGetsTheSameResponseAndNoTokenOrMail() throws Exception {
        requestReset("nobody@example.com");

        assertThat(tokens.count()).isZero();
        Mockito.verifyNoInteractions(mailSender);
    }
}
