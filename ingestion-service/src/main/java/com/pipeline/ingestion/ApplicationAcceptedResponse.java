package com.pipeline.ingestion;

public record ApplicationAcceptedResponse(
        String candidateId,
        CandidateLifecycleStatus status
) {
}
