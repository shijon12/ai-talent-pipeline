package com.pipeline.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoringOutboxPublisher {

    static final String QUALIFIED_BINDING = "qualifiedProducer-out-0";
    static final String LIFECYCLE_BINDING = "lifecycleProducer-out-0";

    private final ScoringOutboxEventRepository repository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Scheduled(
            fixedDelayString = "${pipeline.outbox.fixed-delay-ms:1000}"
    )
    @Transactional
    public void publishPending() {
        for (ScoringOutboxEvent event
                : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            publish(event);
        }
    }

    private void publish(ScoringOutboxEvent outboxEvent) {
        try {
            Object payload = deserialize(outboxEvent);
            boolean sent = streamBridge.send(
                    binding(outboxEvent.getEventType()),
                    MessageBuilder.withPayload(payload)
                            .setHeader(
                                    KafkaHeaders.KEY,
                                    outboxEvent.getPartitionKey()
                                            .getBytes(StandardCharsets.UTF_8)
                            )
                            .build()
            );
            if (!sent) {
                throw new IllegalStateException(
                        "StreamBridge rejected the scoring outbox event"
                );
            }
            outboxEvent.markPublished(Instant.now());
        }
        catch (JsonProcessingException | RuntimeException exception) {
            outboxEvent.markFailed(exception.getMessage());
            log.warn(
                    "Scoring outbox publication failed eventId={} attempt={}: {}",
                    outboxEvent.getEventId(),
                    outboxEvent.getAttemptCount(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private Object deserialize(ScoringOutboxEvent event)
            throws JsonProcessingException {
        return switch (event.getEventType()) {
            case CANDIDATE_QUALIFIED -> objectMapper.readValue(
                    event.getPayloadJson(),
                    CandidateQualifiedEvent.class
            );
            case CANDIDATE_LIFECYCLE -> objectMapper.readValue(
                    event.getPayloadJson(),
                    CandidateLifecycleEvent.class
            );
        };
    }

    private String binding(ScoringOutboxEventType eventType) {
        return switch (eventType) {
            case CANDIDATE_QUALIFIED -> QUALIFIED_BINDING;
            case CANDIDATE_LIFECYCLE -> LIFECYCLE_BINDING;
        };
    }
}
