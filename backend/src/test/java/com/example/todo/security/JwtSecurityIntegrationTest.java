package com.example.todo.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.UserRepository;
import com.example.todo.support.PostgresTestSupport;
import com.example.todo.support.TestSecrets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestSupport.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class JwtSecurityIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwtService;

    User user;
    User admin;

    @BeforeEach
    void seed() {
        users.deleteAll();
        user = users.save(new User("Ana", "ana@example.com", "hash", Role.USER));
        admin = users.save(new User("Boss", "boss@example.com", "hash", Role.ADMIN));
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.issue(u).token();
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/_test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/_test/me").header("Authorization", "Bearer nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        JwtService past = new JwtService(TestSecrets.JWT_SECRET,
                Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC));
        mvc.perform(get("/api/v1/_test/me").header("Authorization", "Bearer " + past.issue(user).token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForDeletedAccountIs401() throws Exception {
        String token = bearer(user);
        users.delete(user);

        mvc.perform(get("/api/v1/_test/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIs200AndExposesCurrentUser() throws Exception {
        mvc.perform(get("/api/v1/_test/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(user.getId().toString()));
    }

    @Test
    void userTokenOnAdminEndpointIs403() throws Exception {
        mvc.perform(get("/api/v1/_test/admin").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminTokenOnAdminEndpointIs200() throws Exception {
        mvc.perform(get("/api/v1/_test/admin").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    void authEndpointsArePublic() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
