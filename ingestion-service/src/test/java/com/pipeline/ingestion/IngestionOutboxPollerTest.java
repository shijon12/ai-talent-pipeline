package com.pipeline.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

class IngestionOutboxPollerTest {

    private TransactionalOutboxRepository repository;
    private StreamBridge streamBridge;
    private IngestionOutboxPoller poller;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(
                TransactionalOutboxRepository.class
        );
        streamBridge = org.mockito.Mockito.mock(StreamBridge.class);
        poller = new IngestionOutboxPoller(repository, streamBridge);
    }

    @Test
    void routesBothEventTypesAndMarksAcknowledgedMessagesProcessed() {
        TransactionalOutbox application = event(
                IngestionOutboxEventType.APPLICATION_INGESTED
        );
        TransactionalOutbox lifecycle = event(
                IngestionOutboxEventType.CANDIDATE_LIFECYCLE
        );
        when(repository.lockOldestPending())
                .thenReturn(List.of(application, lifecycle));
        when(streamBridge.send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Message.class)
        )).thenReturn(true);

        poller.publishPendingEvents();

        verify(streamBridge).send(
                eq("candidateProducer-out-0"),
                org.mockito.ArgumentMatchers.any(Message.class)
        );
        ArgumentCaptor<Message<?>> lifecycleMessage =
                ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(
                eq("lifecycleProducer-out-0"),
                lifecycleMessage.capture()
        );
        assertThat(lifecycleMessage.getValue().getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo("candidate-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(application.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(lifecycle.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(application.getAttemptCount()).isOne();
    }

    @Test
    void rejectedMessageRemainsPendingForRetry() {
        TransactionalOutbox event = event(
                IngestionOutboxEventType.APPLICATION_INGESTED
        );
        when(repository.lockOldestPending()).thenReturn(List.of(event));
        when(streamBridge.send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Message.class)
        )).thenReturn(false);

        poller.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isOne();
        assertThat(event.getLastError()).contains("rejected");
    }

    private TransactionalOutbox event(IngestionOutboxEventType type) {
        return new TransactionalOutbox(
                UUID.randomUUID(),
                "CandidateApplication",
                "candidate-1",
                type,
                "{\"id\":\"candidate-1\"}",
                Instant.now()
        );
    }
}
