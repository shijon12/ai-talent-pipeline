package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

class ScoringOutboxPublisherTest {

    private ScoringOutboxEventRepository repository;
    private StreamBridge streamBridge;
    private ObjectMapper objectMapper;
    private ScoringOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository =
                org.mockito.Mockito.mock(ScoringOutboxEventRepository.class);
        streamBridge = org.mockito.Mockito.mock(StreamBridge.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher = new ScoringOutboxPublisher(
                repository,
                streamBridge,
                objectMapper
        );
    }

    @Test
    void routesQualifiedAndLifecycleEventsToTheirBindings()
            throws Exception {
        ScoringOutboxEvent qualified = qualifiedEvent();
        ScoringOutboxEvent lifecycle = lifecycleEvent();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(qualified, lifecycle));
        when(streamBridge.send(any(), any(Message.class))).thenReturn(true);

        publisher.publishPending();

        verify(streamBridge).send(
                eq(ScoringOutboxPublisher.QUALIFIED_BINDING),
                any(Message.class)
        );
        verify(streamBridge).send(
                eq(ScoringOutboxPublisher.LIFECYCLE_BINDING),
                any(Message.class)
        );
        assertThat(qualified.getPublishedAt()).isNotNull();
        assertThat(lifecycle.getPublishedAt()).isNotNull();
    }

    @Test
    void retainsRejectedEventForRetry() throws Exception {
        ScoringOutboxEvent lifecycle = lifecycleEvent();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(lifecycle));
        when(streamBridge.send(
                eq(ScoringOutboxPublisher.LIFECYCLE_BINDING),
                any(Message.class)
        )).thenReturn(false);

        publisher.publishPending();

        assertThat(lifecycle.getPublishedAt()).isNull();
        assertThat(lifecycle.getAttemptCount()).isEqualTo(1);
        assertThat(lifecycle.getLastError()).contains("rejected");
    }

    private ScoringOutboxEvent qualifiedEvent() throws Exception {
        CandidateQualifiedEvent payload = new CandidateQualifiedEvent(
                "candidate-001",
                "Test Candidate",
                "candidate@example.com",
                81,
                "job-101"
        );
        return new ScoringOutboxEvent(
                "f075fdda-06bd-37e0-a2b6-b9bcf0074e77",
                payload.candidateId(),
                ScoringOutboxEventType.CANDIDATE_QUALIFIED,
                objectMapper.writeValueAsString(payload),
                Instant.now()
        );
    }

    private ScoringOutboxEvent lifecycleEvent() throws Exception {
        CandidateLifecycleEvent payload = new CandidateLifecycleEvent(
                "56d6f24b-1596-316c-bab0-16521c98175c",
                "candidate-001",
                CandidateLifecycleStatus.SCORED,
                Instant.now(),
                "scoring-service"
        );
        return new ScoringOutboxEvent(
                payload.eventId(),
                payload.candidateId(),
                ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                objectMapper.writeValueAsString(payload),
                payload.occurredAt()
        );
    }
}
