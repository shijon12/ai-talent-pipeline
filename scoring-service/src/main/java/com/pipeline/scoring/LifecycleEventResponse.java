package com.pipeline.scoring;

import java.time.Instant;

public record LifecycleEventResponse(
        String eventId,
        String candidateId,
        CandidateLifecycleStatus status,
        Instant occurredAt,
        String source
) {
    public static LifecycleEventResponse from(
            CandidateLifecycleEventEntity event
    ) {
        return new LifecycleEventResponse(
                event.getEventId(),
                event.getCandidateId(),
                event.getStatus(),
                event.getOccurredAt(),
                event.getSource()
        );
    }
}
