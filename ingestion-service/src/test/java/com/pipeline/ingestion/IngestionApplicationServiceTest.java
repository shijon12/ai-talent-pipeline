package com.pipeline.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IngestionApplicationServiceTest {

    private TransactionalOutboxRepository outboxRepository;
    private CandidateLifecycleTrailRepository lifecycleRepository;
    private IngestionApplicationService service;

    @BeforeEach
    void setUp() {
        outboxRepository = org.mockito.Mockito.mock(
                TransactionalOutboxRepository.class
        );
        lifecycleRepository = org.mockito.Mockito.mock(
                CandidateLifecycleTrailRepository.class
        );
        service = new IngestionApplicationService(
                outboxRepository,
                lifecycleRepository,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void atomicallyStagesCandidateAndLifecycleEvents() {
        ApplicationAcceptedResponse response = service.accept(
                " Test Candidate ",
                " candidate@example.com ",
                "Java and Kafka",
                " job-101 "
        );

        ArgumentCaptor<CandidateLifecycleTrail> lifecycle =
                ArgumentCaptor.forClass(CandidateLifecycleTrail.class);
        verify(lifecycleRepository).save(lifecycle.capture());
        assertThat(lifecycle.getValue().getCandidateId())
                .isEqualTo(response.candidateId());
        assertThat(lifecycle.getValue().getStatus())
                .isEqualTo(CandidateLifecycleStatus.INGESTED);

        ArgumentCaptor<TransactionalOutbox> outbox =
                ArgumentCaptor.forClass(TransactionalOutbox.class);
        verify(outboxRepository, org.mockito.Mockito.times(2))
                .save(outbox.capture());
        List<TransactionalOutbox> events = outbox.getAllValues();
        assertThat(events)
                .extracting(TransactionalOutbox::getEventType)
                .containsExactly(
                        IngestionOutboxEventType.APPLICATION_INGESTED,
                        IngestionOutboxEventType.CANDIDATE_LIFECYCLE
                );
        assertThat(events)
                .allSatisfy(event -> {
                    assertThat(event.getAggregateId())
                            .isEqualTo(response.candidateId());
                    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
                    assertThat(event.getPayload()).contains(response.candidateId());
                });
    }
}
