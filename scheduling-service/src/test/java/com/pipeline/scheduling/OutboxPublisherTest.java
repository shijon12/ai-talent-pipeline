package com.pipeline.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

class OutboxPublisherTest {

    private OutboxEventRepository repository;
    private StreamBridge streamBridge;
    private ObjectMapper objectMapper;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(OutboxEventRepository.class);
        streamBridge = org.mockito.Mockito.mock(StreamBridge.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher = new OutboxPublisher(repository, streamBridge, objectMapper);
    }

    @Test
    void marksEventPublishedAfterKafkaAcceptsIt() throws Exception {
        OutboxEvent event = pendingEvent();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(streamBridge.send(
                eq(OutboxPublisher.LIFECYCLE_OUTPUT_BINDING),
                any(Message.class)
        )).thenReturn(true);

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void retainsEventForRetryWhenKafkaRejectsIt() throws Exception {
        OutboxEvent event = pendingEvent();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(streamBridge.send(
                eq(OutboxPublisher.LIFECYCLE_OUTPUT_BINDING),
                any(Message.class)
        )).thenReturn(false);

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("rejected");
    }

    private OutboxEvent pendingEvent() throws Exception {
        CandidateLifecycleEvent payload = new CandidateLifecycleEvent(
                "131cbb9a-6390-360c-8595-50a28250d3bf",
                "candidate-008",
                CandidateLifecycleStatus.OUTREACH_SENT,
                Instant.now(),
                "scheduling-service"
        );
        return new OutboxEvent(
                payload,
                objectMapper.writeValueAsString(payload)
        );
    }
}
