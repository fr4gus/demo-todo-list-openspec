package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todo.dto.RegisterRequest;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.exception.EmailInUseException;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    UserRepository users;

    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(users, encoder);
    }

    @Test
    void createsUserWithHashedPasswordLowercaseEmailAndUserRole() {
        when(users.existsByEmail("ana@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(new RegisterRequest("  Ana Perez ", "Ana@Example.com", "s3cretpass"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        User user = saved.getValue();
        assertThat(user.getFullName()).isEqualTo("Ana Perez");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getPasswordHash()).isNotEqualTo("s3cretpass").startsWith("$2");
        assertThat(encoder.matches("s3cretpass", user.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "s3cretpass")))
                .isInstanceOf(EmailInUseException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCaseVariantOfExistingEmail() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "Ana@Example.com", "s3cretpass")))
                .isInstanceOf(EmailInUseException.class)
                .extracting("code").isEqualTo("EMAIL_IN_USE");
    }

    @Test
    void concurrentDuplicateSurfacesAsEmailInUse() {
        when(users.existsByEmail("ana@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@example.com", "s3cretpass")))
                .isInstanceOf(EmailInUseException.class);
    }
}
