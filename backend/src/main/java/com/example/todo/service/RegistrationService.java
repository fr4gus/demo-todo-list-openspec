package com.example.todo.service;

import com.example.todo.dto.RegisterRequest;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.exception.EmailInUseException;
import com.example.todo.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Always creates a {@link Role#USER}; administrators exist only through startup seeding. */
    @Transactional
    public User register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailInUseException();
        }
        User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailInUseException(); // lost a race with a concurrent registration
        }
    }
}
