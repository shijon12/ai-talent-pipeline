package com.pipeline.ingestion;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionOutboxPoller {

    private static final String CANDIDATE_BINDING = "candidateProducer-out-0";
    private static final String LIFECYCLE_BINDING = "lifecycleProducer-out-0";
    private static final int MAXIMUM_ATTEMPTS = 10;

    private final TransactionalOutboxRepository outboxRepository;
    private final StreamBridge streamBridge;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        for (TransactionalOutbox event : outboxRepository.lockOldestPending()) {
            try {
                boolean acknowledged = streamBridge.send(
                        bindingFor(event.getEventType()),
                        MessageBuilder.withPayload(event.getPayload())
                                .setHeader(
                                        KafkaHeaders.KEY,
                                        event.getAggregateId().getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                )
                                .setHeader(
                                        "contentType",
                                        MediaType.APPLICATION_JSON_VALUE
                                )
                                .build()
                );
                if (!acknowledged) {
                    throw new IllegalStateException(
                            "Kafka binder rejected the outbox message"
                    );
                }
                event.markProcessed(Instant.now());
            }
            catch (Exception exception) {
                event.markFailed(
                        exception.getMessage() == null
                                ? exception.getClass().getSimpleName()
                                : exception.getMessage(),
                        MAXIMUM_ATTEMPTS
                );
                log.warn(
                        "Ingestion outbox publication failed id={} attempt={}: {}",
                        event.getId(),
                        event.getAttemptCount(),
                        exception.getClass().getSimpleName()
                );
            }
        }
    }

    private String bindingFor(IngestionOutboxEventType eventType) {
        return switch (eventType) {
            case APPLICATION_INGESTED -> CANDIDATE_BINDING;
            case CANDIDATE_LIFECYCLE -> LIFECYCLE_BINDING;
        };
    }
}
