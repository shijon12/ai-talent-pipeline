package com.pipeline.ingestion;

public record CandidateApplication(
        String id,
        String name,
        String email,
        String resumeText,
        String targetJobId
) {
}
