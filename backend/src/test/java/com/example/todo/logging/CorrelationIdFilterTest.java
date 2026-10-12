package com.example.todo.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.OutputStreamAppender;
import com.example.todo.support.ApiSliceTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@ApiSliceTest
class CorrelationIdFilterTest {

    private static final String BEARER = "Bearer super-secret-token-value";

    @Autowired
    MockMvc mvc;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private Logger root;
    private OutputStreamAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;

    @BeforeEach
    void attachJsonAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(ctx);
        encoder.start();
        appender = new OutputStreamAppender<>();
        appender.setContext(ctx);
        appender.setEncoder(encoder);
        appender.setOutputStream(out);
        appender.start();
        root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
    }

    @AfterEach
    void detach() {
        root.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logLinesAreJsonWithCorrelationIdAndNoSecrets() throws Exception {
        var response = mvc.perform(get("/api/v1/health")
                        .header("X-Request-Id", "req-123")
                        .header("Authorization", BEARER))
                .andReturn().getResponse();

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-123");
        String logs = out.toString(StandardCharsets.UTF_8);
        String[] lines = logs.strip().split("\n");
        assertThat(lines).isNotEmpty();
        ObjectMapper mapper = new ObjectMapper();
        boolean sawRequestLine = false;
        for (String line : lines) {
            JsonNode json = mapper.readTree(line);
            assertThat(json.has("@timestamp")).isTrue();
            assertThat(json.get("level").asText()).isNotBlank();
            assertThat(json.get("logger_name").asText()).isNotBlank();
            assertThat(json.get("message").asText()).isNotNull();
            assertThat(json.get("correlationId").asText()).isEqualTo("req-123");
            sawRequestLine |= json.get("message").asText().contains("/api/v1/health");
        }
        assertThat(sawRequestLine).isTrue();
        assertThat(logs).doesNotContain("super-secret-token-value").doesNotContain("Authorization");
    }

    @Test
    void generatesIdWhenMissingOrUnsafe() throws Exception {
        var generated = mvc.perform(get("/api/v1/health")).andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(generated).matches("[0-9a-f-]{36}");

        var replaced = mvc.perform(get("/api/v1/health").header("X-Request-Id", "bad id\twith spaces"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(replaced).matches("[0-9a-f-]{36}");
    }
}
