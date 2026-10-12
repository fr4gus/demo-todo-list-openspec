package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.todo.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PasswordResetMailerTest {

    @Mock
    JavaMailSender mailSender;

    PasswordResetMailer mailer() {
        return new PasswordResetMailer(mailSender,
                new AppProperties("secret-secret-secret-secret-secret!", "http://localhost:5173", "no-reply@todo.local"));
    }

    @Test
    void emailLinksToTheFrontendResetRouteWithTheToken() {
        mailer().send(new PasswordResetRequested("ana@example.com", "Ana", "tok_en-123"));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("ana@example.com");
        assertThat(message.getValue().getFrom()).isEqualTo("no-reply@todo.local");
        assertThat(message.getValue().getText()).contains("http://localhost:5173/reset-password?token=tok_en-123");
    }

    @Test
    void deliveryFailureIsSwallowed() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> mailer().send(new PasswordResetRequested("ana@example.com", "Ana", "tok")))
                .doesNotThrowAnyException();
    }
}
