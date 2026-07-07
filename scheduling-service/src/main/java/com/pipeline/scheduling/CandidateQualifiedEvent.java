package com.pipeline.scheduling;

public record CandidateQualifiedEvent(
        String candidateId,
        String name,
        String email,
        int technicalFitScore,
        String targetJobId
) {
}
