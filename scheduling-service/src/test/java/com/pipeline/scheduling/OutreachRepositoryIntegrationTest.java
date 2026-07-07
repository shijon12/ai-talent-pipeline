package com.pipeline.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.default-schema=scheduling",
        "spring.flyway.schemas=scheduling",
        "spring.jpa.properties.hibernate.default_schema=scheduling"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OutreachRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private OutreachAttemptRepository repository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Test
    void persistsOnlyOneOutreachAttemptForDuplicateCandidateEvents() {
        OutreachService service = new OutreachService(
                repository,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        CandidateQualifiedEvent event = new CandidateQualifiedEvent(
                "candidate-008",
                "Morgan Chen",
                "morgan@example.com",
                77,
                "job-101"
        );

        OutreachAttempt first = service.sendMockInvite(event);
        OutreachAttempt duplicate = service.sendMockInvite(event);

        assertThat(duplicate.getAttemptId()).isEqualTo(first.getAttemptId());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByCandidateId("candidate-008"))
                .get()
                .extracting(OutreachAttempt::getStatus)
                .isEqualTo(OutreachAttemptStatus.SENT);
        assertThat(outboxRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(outbox -> {
                    assertThat(outbox.getAggregateId())
                            .isEqualTo("candidate-008");
                    assertThat(outbox.getPublishedAt()).isNull();
                    assertThat(outbox.getPayloadJson())
                            .contains("OUTREACH_SENT");
                });
    }
}
