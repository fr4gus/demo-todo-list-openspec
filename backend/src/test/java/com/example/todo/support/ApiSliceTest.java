package com.example.todo.support;

import com.example.todo.config.PropertiesConfig;
import com.example.todo.controller.HealthController;
import com.example.todo.logging.CorrelationIdFilter;
import com.example.todo.security.ProblemDetailSecurityHandlers;
import com.example.todo.security.SecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Web slice with the platform wiring (security, error handling, CORS, logging filter) and no database. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(controllers = {HealthController.class, SampleErrorController.class})
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, PropertiesConfig.class, CorrelationIdFilter.class})
@TestPropertySource(properties = {
        "app.jwt-secret=test-secret-test-secret-test-secret-123456",
        "app.frontend-origin=http://frontend.test",
        "spring.mvc.throw-exception-if-no-handler-found=true",
        "spring.web.resources.add-mappings=false"
})
public @interface ApiSliceTest {
}
