package com.pipeline.scoring;

public record CandidateQualifiedEvent(
        String candidateId,
        String name,
        String email,
        int technicalFitScore,
        String targetJobId
) {
}
