package com.example.todo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void contextFailsNamingMissingJwtSecret() {
        runner.withPropertyValues("app.jwt-secret=", "app.frontend-origin=http://x").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(rootMessages(ctx.getStartupFailure())).contains("JWT_SECRET");
        });
    }

    @Test
    void contextFailsNamingMissingFrontendOrigin() {
        runner.withPropertyValues("app.jwt-secret=abc", "app.frontend-origin=").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(rootMessages(ctx.getStartupFailure())).contains("FRONTEND_ORIGIN");
        });
    }

    @Test
    void contextStartsWhenRequiredValuesPresent() {
        runner.withPropertyValues("app.jwt-secret=abc", "app.frontend-origin=http://x")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    private static String rootMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getMessage()).append('\n');
        }
        return sb.toString();
    }
}
