package com.example.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.config.JpaConfig;
import com.example.todo.entity.PasswordResetToken;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.support.PostgresTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestSupport.class, JpaConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class PasswordResetTokenRepositoryTest {

    @Autowired
    UserRepository users;
    @Autowired
    PasswordResetTokenRepository tokens;
    @Autowired
    JdbcTemplate jdbc;

    private User newUser(String email) {
        return users.saveAndFlush(new User("Ana", email, "hash", Role.USER));
    }

    @Test
    void savesAndFindsByHash() {
        User user = newUser("a@example.com");
        tokens.saveAndFlush(new PasswordResetToken(user.getId(), "hash-1", Instant.now().plusSeconds(3600)));

        PasswordResetToken found = tokens.findByTokenHash("hash-1").orElseThrow();
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getUsedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(tokens.findByTokenHash("other")).isEmpty();
    }

    @Test
    void tokenHashMustBeUnique() {
        User user = newUser("a@example.com");
        tokens.saveAndFlush(new PasswordResetToken(user.getId(), "same", Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> tokens.saveAndFlush(new PasswordResetToken(user.getId(), "same", Instant.now().plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tokenMustReferenceAnExistingUser() {
        assertThatThrownBy(() -> tokens.saveAndFlush(
                new PasswordResetToken(java.util.UUID.randomUUID(), "orphan", Instant.now().plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheUserCascadesToTheirTokens() {
        User user = newUser("a@example.com");
        tokens.saveAndFlush(new PasswordResetToken(user.getId(), "h1", Instant.now().plusSeconds(60)));
        tokens.saveAndFlush(new PasswordResetToken(user.getId(), "h2", Instant.now().plusSeconds(60)));

        jdbc.update("DELETE FROM users WHERE id = ?", user.getId());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens", Integer.class)).isZero();
    }

    @Test
    void markAllUnusedAsUsedOnlyTouchesThatUsersUnusedTokens() {
        User ana = newUser("a@example.com");
        User bob = newUser("b@example.com");
        tokens.saveAndFlush(new PasswordResetToken(ana.getId(), "ana-1", Instant.now().plusSeconds(60)));
        PasswordResetToken used = new PasswordResetToken(ana.getId(), "ana-used", Instant.now().plusSeconds(60));
        Instant earlier = Instant.now().minusSeconds(100);
        used.markUsed(earlier);
        tokens.saveAndFlush(used);
        tokens.saveAndFlush(new PasswordResetToken(bob.getId(), "bob-1", Instant.now().plusSeconds(60)));

        int updated = tokens.markAllUnusedAsUsed(ana.getId(), Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(tokens.findByTokenHash("ana-1").orElseThrow().getUsedAt()).isNotNull();
        assertThat(tokens.findByTokenHash("bob-1").orElseThrow().getUsedAt()).isNull();
        assertThat(tokens.findByTokenHash("ana-used").orElseThrow().getUsedAt()).isCloseTo(earlier, org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
    }
}
