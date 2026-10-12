package com.example.todo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.todo.repository.UserRepository;
import com.example.todo.support.PostgresTestSupport;
import com.example.todo.support.TestSecrets;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Registration and login, successful and failing, must never leak passwords, hashes or tokens into logs. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestSupport.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class SensitiveDataLoggingTest {

    static final String PASSWORD = "Sup3r-Secret-Pa55word";

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;

    ListAppender<ILoggingEvent> appender;
    Logger root;
    Level previousLevel;

    @BeforeEach
    void attach() {
        users.deleteAll();
        root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        previousLevel = root.getLevel();
        root.setLevel(Level.DEBUG); // the most verbose level an operator would reasonably enable; TRACE makes JDBC/Hibernate internals print bound values
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void detach() {
        root.detachAppender(appender);
        root.setLevel(previousLevel);
    }

    @Test
    void noSecretsAreLoggedAcrossRegistrationAndLogin() throws Exception {
        String registerBody = "{\"fullName\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"%s\"}".formatted(PASSWORD);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isConflict());

        String hash = users.findByEmail("ana@example.com").orElseThrow().getPasswordHash();
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"wrong-Pa55word\"}"))
                .andExpect(status().isUnauthorized());

        List<String> logged = appender.list.stream().map(e -> e.getLoggerName() + " " + e.getFormattedMessage() + " " + e.getMDCPropertyMap()
                + " " + e.getThrowableProxy()).toList();
        assertThat(logged).isNotEmpty();
        assertThat(logged).noneMatch(m -> m.contains(PASSWORD));
        assertThat(logged).noneMatch(m -> m.contains("wrong-Pa55word"));
        assertThat(logged).noneMatch(m -> m.contains(hash));
        assertThat(logged).noneMatch(m -> m.contains(token));
    }
}
