package com.pipeline.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactional_outbox", schema = "ingestion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionalOutbox {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private IngestionOutboxEventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public TransactionalOutbox(
            UUID id,
            String aggregateType,
            String aggregateId,
            IngestionOutboxEventType eventType,
            String payload,
            Instant createdAt
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void markProcessed(Instant processedAt) {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = processedAt;
        this.attemptCount++;
        this.lastError = null;
    }

    public void markFailed(String error, int maximumAttempts) {
        this.attemptCount++;
        this.lastError = error == null
                ? "Unknown publication failure"
                : error.substring(0, Math.min(error.length(), 500));
        if (attemptCount >= maximumAttempts) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
