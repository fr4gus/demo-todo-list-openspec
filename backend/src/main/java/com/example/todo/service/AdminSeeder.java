package com.example.todo.service;

import com.example.todo.config.AdminSeedProperties;
import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator at startup. All three variables must be set together; a partial
 * configuration or an email already used by a regular user aborts startup. No endpoint creates admins.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminSeedProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminSeedProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(properties.email())) {
            missing.add("ADMIN_EMAIL");
        }
        if (!StringUtils.hasText(properties.password())) {
            missing.add("ADMIN_PASSWORD");
        }
        if (!StringUtils.hasText(properties.fullName())) {
            missing.add("ADMIN_FULL_NAME");
        }

        if (missing.size() == 3) {
            log.warn("ADMIN_EMAIL, ADMIN_PASSWORD and ADMIN_FULL_NAME are not set; no administrator will be created");
            return;
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Incomplete admin configuration; missing: " + String.join(", ", missing));
        }
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }

        String email = User.normalizeEmail(properties.email());
        if (users.existsByEmail(email)) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL belongs to an existing non-admin account; choose another email. Nothing was changed.");
        }
        users.save(new User(properties.fullName().trim(), email, passwordEncoder.encode(properties.password()), Role.ADMIN));
        log.info("Administrator account created");
    }
}
