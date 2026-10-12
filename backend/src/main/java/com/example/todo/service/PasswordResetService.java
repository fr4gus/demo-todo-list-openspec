package com.example.todo.service;

import com.example.todo.dto.PasswordResetConfirmRequest;
import com.example.todo.dto.PasswordResetRequest;
import com.example.todo.entity.PasswordResetToken;
import com.example.todo.entity.User;
import com.example.todo.exception.ResetTokenInvalidException;
import com.example.todo.exception.TooManyRequestsException;
import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final ResetRateLimiter rateLimiter;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, ResetRateLimiter rateLimiter,
            ApplicationEventPublisher events, Clock clock, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.events = events;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns normally for known and unknown emails alike, so the caller cannot tell them apart.
     * The rate limit counts every request, known or not, for the same reason.
     */
    @Transactional
    public void requestReset(PasswordResetRequest request) {
        String email = User.normalizeEmail(request.email());
        if (!rateLimiter.tryAcquire(email)) {
            throw new TooManyRequestsException();
        }
        users.findByEmail(email).ifPresent(this::issueToken);
    }

    /** Validates and consumes the token and sets the new password in one transaction. */
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = tokens.findByTokenHash(hash(request.token()))
                .filter(t -> t.isUsableAt(now))
                .orElseThrow(ResetTokenInvalidException::new);
        if (tokens.consume(token.getId(), now) == 0) { // lost a race with another confirmation
            throw new ResetTokenInvalidException();
        }
        User user = users.findById(token.getUserId()).orElseThrow(ResetTokenInvalidException::new);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private void issueToken(User user) {
        Instant now = clock.instant();
        tokens.markAllUnusedAsUsed(user.getId(), now);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetToken(user.getId(), hash(token), now.plus(TOKEN_LIFETIME)));
        events.publishEvent(new PasswordResetRequested(user.getEmail(), user.getFullName(), token));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
