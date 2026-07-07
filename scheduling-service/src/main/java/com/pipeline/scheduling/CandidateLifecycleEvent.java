package com.pipeline.scheduling;

import java.time.Instant;

public record CandidateLifecycleEvent(
        String eventId,
        String candidateId,
        CandidateLifecycleStatus status,
        Instant occurredAt,
        String source
) {
}
