package com.pipeline.scheduling;

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
public class OutboxPublisher {

    static final String LIFECYCLE_OUTPUT_BINDING = "lifecycleProducer-out-0";

    private final OutboxEventRepository repository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Scheduled(
            fixedDelayString = "${pipeline.outbox.fixed-delay-ms:1000}"
    )
    @Transactional
    public void publishPending() {
        for (OutboxEvent event
                : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            publish(event);
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            CandidateLifecycleEvent payload = objectMapper.readValue(
                    outboxEvent.getPayloadJson(),
                    CandidateLifecycleEvent.class
            );
            boolean sent = streamBridge.send(
                    LIFECYCLE_OUTPUT_BINDING,
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
                        "StreamBridge rejected the outbox event"
                );
            }
            outboxEvent.markPublished(Instant.now());
        }
        catch (JsonProcessingException | RuntimeException exception) {
            outboxEvent.markFailed(exception.getMessage());
            log.warn(
                    "Outbox publication failed eventId={} attempt={}: {}",
                    outboxEvent.getEventId(),
                    outboxEvent.getAttemptCount(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
