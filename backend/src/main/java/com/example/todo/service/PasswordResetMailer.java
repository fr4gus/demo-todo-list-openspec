package com.example.todo.service;

import com.example.todo.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public PasswordResetMailer(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /** A delivery failure is logged and swallowed so the API response stays identical for every email. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetRequested event) {
        try {
            String link = UriComponentsBuilder.fromUriString(properties.frontendOrigin())
                    .path("/reset-password").queryParam("token", event.token()).build().toUriString();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.mailFrom());
            message.setTo(event.email());
            message.setSubject("Reset your password");
            message.setText("Hi " + event.fullName() + ",\n\nUse this link to choose a new password. "
                    + "It works once and expires in 60 minutes:\n\n" + link
                    + "\n\nIf you did not ask for this, you can ignore this email.");
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Could not send password reset email", e);
        }
    }
}
