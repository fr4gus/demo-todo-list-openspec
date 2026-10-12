package com.example.todo.security;

import com.example.todo.config.AppProperties;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Issues and verifies the HS256 access tokens: {@code sub} = user id, {@code role}, 24-hour lifetime. */
@Service
public class JwtService {

    public static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    static final int MIN_SECRET_BYTES = 32;

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record TokenClaims(UUID userId, Role role) {
    }

    private final SecretKey key;
    private final Clock clock;

    @Autowired
    public JwtService(AppProperties properties) {
        this(properties.jwtSecret(), Clock.systemUTC());
    }

    JwtService(String secret, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(TOKEN_LIFETIME);
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** @throws JwtException if the token is malformed, tampered with, expired or lacks the expected claims */
    public TokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenClaims(UUID.fromString(claims.getSubject()), Role.valueOf(claims.get("role", String.class)));
        } catch (IllegalArgumentException e) { // bad subject / role value
            throw new JwtException("Invalid token claims", e);
        }
    }
}
