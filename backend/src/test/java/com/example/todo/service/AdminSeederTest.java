package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todo.config.AdminSeedProperties;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    UserRepository users;

    PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminSeeder seeder(String email, String password, String fullName) {
        return new AdminSeeder(new AdminSeedProperties(email, password, fullName), users, encoder);
    }

    @Test
    void firstStartCreatesHashedAdmin() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        when(users.existsByEmail("admin@example.com")).thenReturn(false);

        seeder("Admin@Example.com", "admin-password", "Site Admin").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getFullName()).isEqualTo("Site Admin");
        assertThat(admin.getPasswordHash()).isNotEqualTo("admin-password");
        assertThat(encoder.matches("admin-password", admin.getPasswordHash())).isTrue();
    }

    @Test
    void existingAdminLeavesEverythingUntouched() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(true);

        seeder("admin@example.com", "admin-password", "Site Admin").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void absentVariablesCreateNothing() {
        seeder("", null, " ").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void partialConfigurationFailsNamingMissingVariables() {
        assertThatThrownBy(() -> seeder("admin@example.com", "", null).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageContaining("ADMIN_FULL_NAME")
                .hasMessageNotContaining("ADMIN_EMAIL");
        verify(users, never()).save(any());
    }

    @Test
    void emailBelongingToRegularUserFailsStartup() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> seeder("ana@example.com", "admin-password", "Ana").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
        verify(users, never()).save(any());
    }
}
