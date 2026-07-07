package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidateScoringServiceTest {

    @Mock
    private CandidateAnalysisService analysisService;

    @Mock
    private ApplicantScoreRepository scoreRepository;

    @Mock
    private CandidateReviewRepository reviewRepository;

    @Mock
    private ScoringOutboxEventRepository outboxRepository;

    private ObjectMapper objectMapper;
    private CandidateScoringService scoringService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        scoringService = new CandidateScoringService(
                new CandidateScoreCalculator(),
                analysisService,
                scoreRepository,
                reviewRepository,
                outboxRepository,
                objectMapper
        );
    }

    @Test
    void persistsStructuredAdvisoryAnalysisAlongsideDeterministicScore()
            throws Exception {
        CandidateApplication candidate = candidate("candidate-10");
        CandidateAnalysis analysis = CandidateAnalysis.completed(
                List.of("Java", "Kafka"),
                List.of("Java"),
                List.of("Spring Boot"),
                List.of("Kafka"),
                List.of("Java and Kafka development"),
                "Backend engineering experience is explicitly described.",
                "llama3.2:1b",
                "job-match-v1"
        );
        when(analysisService.analyze(candidate)).thenReturn(analysis);

        scoringService.scoreAndPersist(candidate);

        ArgumentCaptor<String> analysisJson = ArgumentCaptor.forClass(String.class);
        verify(scoreRepository).upsert(
                eq("candidate-10"),
                eq("Test Candidate"),
                eq("candidate@example.com"),
                eq("job-1"),
                eq("Java and Kafka development"),
                anyInt(),
                any(Instant.class),
                eq("COMPLETED"),
                analysisJson.capture()
        );

        CandidateAnalysis persisted = objectMapper.readValue(
                analysisJson.getValue(),
                CandidateAnalysis.class
        );
        assertThat(persisted).isEqualTo(analysis);
        ArgumentCaptor<ScoringOutboxEvent> outbox =
                ArgumentCaptor.forClass(ScoringOutboxEvent.class);
        verify(outboxRepository, org.mockito.Mockito.times(2))
                .save(outbox.capture());
        assertThat(outbox.getAllValues())
                .extracting(ScoringOutboxEvent::getEventType)
                .containsOnly(ScoringOutboxEventType.CANDIDATE_LIFECYCLE);
        assertThat(outbox.getAllValues())
                .extracting(ScoringOutboxEvent::getPayloadJson)
                .anySatisfy(payload -> assertThat(payload).contains("SCORED"))
                .anySatisfy(payload -> assertThat(payload)
                        .contains("REVIEW_REQUIRED"));
        verify(reviewRepository).save(any(CandidateReview.class));
    }

    @Test
    void persistsUnavailableStatusWhenLocalAiFallsBack() {
        CandidateApplication candidate = candidate("candidate-11");
        when(analysisService.analyze(candidate))
                .thenReturn(CandidateAnalysis.unavailable());

        scoringService.scoreAndPersist(candidate);

        verify(scoreRepository).upsert(
                eq("candidate-11"),
                eq("Test Candidate"),
                eq("candidate@example.com"),
                eq("job-1"),
                eq("Java and Kafka development"),
                anyInt(),
                any(Instant.class),
                eq("UNAVAILABLE"),
                any(String.class)
        );
    }

    @Test
    void routesHighFallbackScoreToHumanReviewWithoutQualification() {
        CandidateApplication candidate = highScoringCandidate(
                "candidate-fallback"
        );
        when(analysisService.analyze(candidate))
                .thenReturn(CandidateAnalysis.unavailable());

        int score = scoringService.scoreAndPersist(candidate);

        assertThat(score)
                .isGreaterThanOrEqualTo(
                        CandidateScoringService.QUALIFICATION_THRESHOLD
                );
        ArgumentCaptor<ScoringOutboxEvent> events =
                ArgumentCaptor.forClass(ScoringOutboxEvent.class);
        verify(outboxRepository, org.mockito.Mockito.times(2))
                .save(events.capture());
        assertThat(events.getAllValues())
                .extracting(ScoringOutboxEvent::getEventType)
                .containsOnly(ScoringOutboxEventType.CANDIDATE_LIFECYCLE);
        assertThat(events.getAllValues().get(1).getPayloadJson())
                .contains("REVIEW_REQUIRED");
    }

    @Test
    void routesCompletedHighScoreToHumanReviewWithoutAutoQualification() {
        CandidateApplication candidate = highScoringCandidate(
                "candidate-qualified"
        );
        when(analysisService.analyze(candidate))
                .thenReturn(CandidateAnalysis.completed(
                        List.of("Java", "Spring", "Kafka", "PostgreSQL"),
                        List.of("Java", "Spring", "Kafka"),
                        List.of(),
                        List.of("PostgreSQL"),
                        List.of("Java Spring Kafka PostgreSQL"),
                        "Relevant technical experience is explicit.",
                        "llama3.2:1b",
                        "job-match-v1"
                ));

        int score = scoringService.scoreAndPersist(candidate);

        assertThat(score)
                .isGreaterThanOrEqualTo(
                        CandidateScoringService.QUALIFICATION_THRESHOLD
                );
        ArgumentCaptor<ScoringOutboxEvent> events =
                ArgumentCaptor.forClass(ScoringOutboxEvent.class);
        verify(outboxRepository, org.mockito.Mockito.times(2))
                .save(events.capture());
        assertThat(events.getAllValues())
                .extracting(ScoringOutboxEvent::getEventType)
                .containsExactly(
                        ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                        ScoringOutboxEventType.CANDIDATE_LIFECYCLE
                );
        assertThat(events.getAllValues().get(0).getPayloadJson())
                .contains("SCORED");
        assertThat(events.getAllValues().get(1).getPayloadJson())
                .contains("REVIEW_REQUIRED");
    }

    @Test
    void rejectsMalformedEventBeforeCallingAiOrDatabase() {
        CandidateApplication candidate = candidate("");

        assertThatThrownBy(() -> scoringService.scoreAndPersist(candidate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");

        verify(analysisService, never()).analyze(any());
        verify(reviewRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
        verify(scoreRepository, never()).upsert(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any(),
                any()
        );
    }

    private CandidateApplication candidate(String id) {
        return new CandidateApplication(
                id,
                "Test Candidate",
                "candidate@example.com",
                "Java and Kafka development",
                "job-1"
        );
    }

    private CandidateApplication highScoringCandidate(String id) {
        return new CandidateApplication(
                id,
                "Qualified Candidate",
                "qualified@example.com",
                "Java Spring Kafka PostgreSQL microservice production experience "
                        + "with Spring Boot, Docker, distributed systems, and APIs.",
                "job-1"
        );
    }
}
