package com.pipeline.scoring;

public record CandidateApplication(
        String id,
        String name,
        String email,
        String resumeText,
        String targetJobId
) {
}
