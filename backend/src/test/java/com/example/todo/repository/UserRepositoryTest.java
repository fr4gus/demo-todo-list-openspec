package com.example.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.config.JpaConfig;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.support.PostgresTestSupport;
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
class UserRepositoryTest {

    @Autowired
    UserRepository users;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void savesAndFindsByEmailWithAuditColumns() {
        users.saveAndFlush(new User("Ana Perez", "Ana@Example.com", "hash", Role.USER));

        User found = users.findByEmail("ana@example.com").orElseThrow();
        assertThat(found.getFullName()).isEqualTo("Ana Perez");
        assertThat(found.getRole()).isEqualTo(Role.USER);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void duplicateEmailIsRejectedByTheDatabase() {
        users.saveAndFlush(new User("Ana", "ana@example.com", "hash", Role.USER));

        assertThatThrownBy(() -> users.saveAndFlush(new User("Other Ana", "ana@example.com", "hash2", Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mixedCaseEmailCannotBypassUniquenessAtDatabaseLevel() {
        users.saveAndFlush(new User("Ana", "ana@example.com", "hash", Role.USER));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (id, full_name, email, password_hash, role) VALUES (gen_random_uuid(), 'X', 'Ana@Example.com', 'h', 'USER')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unknownRoleIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (id, full_name, email, password_hash, role) VALUES (gen_random_uuid(), 'X', 'x@example.com', 'h', 'ROOT')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByRoleReflectsStoredAccounts() {
        assertThat(users.existsByRole(Role.ADMIN)).isFalse();
        users.saveAndFlush(new User("Boss", "boss@example.com", "hash", Role.ADMIN));
        assertThat(users.existsByRole(Role.ADMIN)).isTrue();
    }
}
