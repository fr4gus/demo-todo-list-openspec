package com.example.todo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    static final String SECRET = com.example.todo.support.TestSecrets.JWT_SECRET;
    static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    private JwtService serviceAt(Instant instant) {
        return new JwtService(SECRET, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private final User user = new User("Ana", "ana@example.com", "hash", Role.ADMIN);

    @Test
    void tokenCarriesUserIdRoleAndExpiresAfter24Hours() {
        JwtService.IssuedToken issued = serviceAt(NOW).issue(user);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        JwtService.TokenClaims claims = serviceAt(NOW).parse(issued.token());
        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void tokenIsRejectedOnceExpired() {
        String token = serviceAt(NOW).issue(user).token();

        assertThat(serviceAt(NOW.plus(Duration.ofHours(23))).parse(token).userId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> serviceAt(NOW.plus(Duration.ofHours(24)).plusSeconds(1)).parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = serviceAt(NOW).issue(user).token();
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + user.getId() + "\",\"role\":\"ADMIN\",\"exp\":9999999999}").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> serviceAt(NOW).parse(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = Jwts.builder().subject(user.getId().toString()).claim("role", "ADMIN")
                .expiration(Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor("another-secret-another-secret-another!".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> serviceAt(NOW).parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageAndUnknownRoleAreRejected() {
        assertThatThrownBy(() -> serviceAt(NOW).parse("not-a-jwt")).isInstanceOf(JwtException.class);
        String badRole = Jwts.builder().subject(user.getId().toString()).claim("role", "ROOT")
                .expiration(Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThatThrownBy(() -> serviceAt(NOW).parse(badRole)).isInstanceOf(JwtException.class);
    }

    @Test
    void secretShorterThan32BytesIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService("", Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
    }
}
