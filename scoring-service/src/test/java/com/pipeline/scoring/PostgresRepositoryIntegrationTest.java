package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostgresRepositoryIntegrationTest {

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
    private ApplicantScoreRepository scoreRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateLifecycleEventRepository lifecycleRepository;

    @Autowired
    private ScoringOutboxEventRepository outboxRepository;

    @Autowired
    private CandidateReviewRepository reviewRepository;

    @Test
    @Transactional
    void flywaySchemaSupportsJobsAndIdempotentScoreUpserts() {
        jobRepository.save(new Job(
                "job-101",
                "Java Engineer",
                "Build Spring Boot services",
                "[\"Java 17\",\"Spring Boot\"]",
                "[\"Kafka\"]",
                Instant.now()
        ));

        scoreRepository.upsert(
                "candidate-001",
                "Alex Morgan",
                "alex@example.com",
                "job-101",
                "Java 17 Spring Boot Kafka",
                70,
                Instant.now(),
                CandidateAnalysis.Status.COMPLETED.name(),
                "{}"
        );
        scoreRepository.upsert(
                "candidate-001",
                "Alex Morgan",
                "alex@example.com",
                "job-101",
                "Java 17 Spring Boot Kafka",
                78,
                Instant.now(),
                CandidateAnalysis.Status.COMPLETED.name(),
                "{}"
        );

        assertThat(jobRepository.findById("job-101")).isPresent();
        assertThat(scoreRepository.findById("candidate-001"))
                .get()
                .extracting(ApplicantScore::getScore)
                .isEqualTo(78);
        assertThat(scoreRepository.count()).isEqualTo(1);

        reviewRepository.save(new CandidateReview(
                "candidate-001",
                Instant.now()
        ));
        assertThat(reviewRepository
                .findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING))
                .extracting(CandidateReview::getCandidateId)
                .containsExactly("candidate-001");

        CandidateLifecycleEvent lifecycleEvent = new CandidateLifecycleEvent(
                "470d8b04-b3c8-36c4-9e50-8419d57128ec",
                "candidate-001",
                CandidateLifecycleStatus.SCORED,
                Instant.now(),
                "scoring-service"
        );
        lifecycleRepository.save(
                new CandidateLifecycleEventEntity(lifecycleEvent)
        );
        assertThat(lifecycleRepository
                .findByCandidateIdOrderByOccurredAtAsc("candidate-001"))
                .extracting(CandidateLifecycleEventEntity::getStatus)
                .containsExactly(CandidateLifecycleStatus.SCORED);

        outboxRepository.save(new ScoringOutboxEvent(
                "f075fdda-06bd-37e0-a2b6-b9bcf0074e77",
                "candidate-001",
                ScoringOutboxEventType.CANDIDATE_QUALIFIED,
                "{}",
                Instant.now()
        ));
        assertThat(outboxRepository
                .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .extracting(ScoringOutboxEvent::getEventType)
                .containsExactly(
                        ScoringOutboxEventType.CANDIDATE_QUALIFIED
                );
    }
}
