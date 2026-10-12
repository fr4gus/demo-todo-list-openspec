package com.example.testsupport.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.config.JpaConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = AuditedSample.class)
@Import(JpaConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuditedEntityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestEntityManager em;

    @Test
    void createdAtIsStableAndUpdatedAtAdvancesOnUpdate() throws Exception {
        AuditedSample saved = em.persistAndFlush(new AuditedSample("first"));
        Instant created = saved.getCreatedAt();
        Instant updated = saved.getUpdatedAt();
        assertThat(created).isNotNull();
        assertThat(updated).isNotNull();

        Thread.sleep(20);
        saved.setName("second");
        em.flush();
        em.clear();

        AuditedSample reloaded = em.find(AuditedSample.class, saved.getId());
        assertThat(reloaded.getCreatedAt()).isEqualTo(created);
        assertThat(reloaded.getUpdatedAt()).isAfter(updated);
    }
}
