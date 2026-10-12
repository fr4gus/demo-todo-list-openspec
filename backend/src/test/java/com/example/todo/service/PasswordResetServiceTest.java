package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.todo.dto.PasswordResetConfirmRequest;
import com.example.todo.dto.PasswordResetRequest;
import com.example.todo.exception.ResetTokenInvalidException;
import com.example.todo.entity.PasswordResetToken;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.exception.TooManyRequestsException;
import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    @Mock
    UserRepository users;
    @Mock
    PasswordResetTokenRepository tokens;
    @Mock
    ResetRateLimiter rateLimiter;
    @Mock
    ApplicationEventPublisher events;

    PasswordResetService service;
    final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final User ana = new User("Ana Perez", "ana@example.com", "hash", Role.USER);

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(users, tokens, rateLimiter, events, Clock.fixed(NOW, ZoneOffset.UTC), encoder);
    }

    @Test
    void knownEmailStoresOnlyTheHashSupersedesOldTokensAndPublishesTheRawToken() {
        when(rateLimiter.tryAcquire("ana@example.com")).thenReturn(true);
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(ana));

        service.requestReset(new PasswordResetRequest("  Ana@Example.com "));

        verify(tokens).markAllUnusedAsUsed(ana.getId(), NOW);
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        ArgumentCaptor<PasswordResetRequested> event = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events).publishEvent(event.capture());

        String rawToken = event.getValue().token();
        assertThat(rawToken).hasSize(43).matches("[A-Za-z0-9_-]+"); // 32 bytes, URL-safe Base64, no padding
        assertThat(event.getValue().email()).isEqualTo("ana@example.com");
        assertThat(saved.getValue().getUserId()).isEqualTo(ana.getId());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
        assertThat(saved.getValue()).extracting("tokenHash")
                .isEqualTo(PasswordResetService.hash(rawToken))
                .isNotEqualTo(rawToken);
    }

    @Test
    void unknownEmailStoresNothingAndSendsNothing() {
        when(rateLimiter.tryAcquire("nobody@example.com")).thenReturn(true);
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.requestReset(new PasswordResetRequest("nobody@example.com"));

        verify(tokens, never()).save(any());
        verify(tokens, never()).markAllUnusedAsUsed(any(), any());
        verifyNoInteractions(events);
    }

    @Test
    void rateLimitedRequestIsRejectedBeforeAnyLookup() {
        when(rateLimiter.tryAcquire("ana@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.requestReset(new PasswordResetRequest("ana@example.com")))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(users, tokens, events);
    }

    @Test
    void twoRequestsProduceDifferentTokens() {
        when(rateLimiter.tryAcquire("ana@example.com")).thenReturn(true);
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(ana));

        service.requestReset(new PasswordResetRequest("ana@example.com"));
        service.requestReset(new PasswordResetRequest("ana@example.com"));

        ArgumentCaptor<PasswordResetRequested> event = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events, org.mockito.Mockito.times(2)).publishEvent(event.capture());
        assertThat(event.getAllValues().get(0).token()).isNotEqualTo(event.getAllValues().get(1).token());
    }

    private PasswordResetToken tokenFor(String raw, Instant expiresAt) {
        return new PasswordResetToken(ana.getId(), PasswordResetService.hash(raw), expiresAt);
    }

    @Test
    void confirmSetsNewHashAndConsumesTheToken() {
        PasswordResetToken token = tokenFor("raw-token", NOW.plusSeconds(60));
        when(tokens.findByTokenHash(PasswordResetService.hash("raw-token"))).thenReturn(Optional.of(token));
        when(tokens.consume(token.getId(), NOW)).thenReturn(1);
        when(users.findById(ana.getId())).thenReturn(Optional.of(ana));

        service.confirmReset(new PasswordResetConfirmRequest("raw-token", "brand-new-pass"));

        assertThat(encoder.matches("brand-new-pass", ana.getPasswordHash())).isTrue();
        assertThat(ana.getPasswordHash()).isNotEqualTo("brand-new-pass");
    }

    @Test
    void confirmRejectsUnknownToken() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest("nope", "brand-new-pass")))
                .isInstanceOf(ResetTokenInvalidException.class).extracting("code").isEqualTo("RESET_TOKEN_INVALID");
        verify(tokens, never()).consume(any(), any());
    }

    @Test
    void confirmRejectsExpiredToken() {
        PasswordResetToken token = tokenFor("old", NOW.minusSeconds(1));
        when(tokens.findByTokenHash(PasswordResetService.hash("old"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest("old", "brand-new-pass")))
                .isInstanceOf(ResetTokenInvalidException.class);
        verify(tokens, never()).consume(any(), any());
        assertThat(ana.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void confirmRejectsAlreadyUsedOrSupersededToken() {
        PasswordResetToken token = tokenFor("used", NOW.plusSeconds(60));
        token.markUsed(NOW.minusSeconds(5));
        when(tokens.findByTokenHash(PasswordResetService.hash("used"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest("used", "brand-new-pass")))
                .isInstanceOf(ResetTokenInvalidException.class);
        assertThat(ana.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void confirmRejectsWhenAConcurrentConfirmationConsumedTheTokenFirst() {
        PasswordResetToken token = tokenFor("raced", NOW.plusSeconds(60));
        when(tokens.findByTokenHash(PasswordResetService.hash("raced"))).thenReturn(Optional.of(token));
        when(tokens.consume(token.getId(), NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest("raced", "brand-new-pass")))
                .isInstanceOf(ResetTokenInvalidException.class);
        assertThat(ana.getPasswordHash()).isEqualTo("hash");
    }
}
