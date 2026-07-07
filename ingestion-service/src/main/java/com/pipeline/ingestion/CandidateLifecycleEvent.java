package com.pipeline.ingestion;

import java.time.Instant;

public record CandidateLifecycleEvent(
        String eventId,
        String candidateId,
        CandidateLifecycleStatus status,
        Instant occurredAt,
        String source
) {
}
