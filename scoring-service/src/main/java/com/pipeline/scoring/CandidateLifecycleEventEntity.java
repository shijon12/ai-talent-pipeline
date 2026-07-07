package com.pipeline.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidate_lifecycle_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CandidateLifecycleEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "candidate_id", nullable = false, length = 100)
    private String candidateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CandidateLifecycleStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 64)
    private String source;

    public CandidateLifecycleEventEntity(CandidateLifecycleEvent event) {
        this.eventId = event.eventId();
        this.candidateId = event.candidateId();
        this.status = event.status();
        this.occurredAt = event.occurredAt();
        this.source = event.source();
    }
}
