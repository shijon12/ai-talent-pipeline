package com.pipeline.scheduling;

import java.time.Instant;

public record OutreachAttemptResponse(
        String attemptId,
        String candidateId,
        String name,
        String email,
        int technicalFitScore,
        String targetJobId,
        OutreachAttemptStatus status,
        Instant attemptedAt
) {
    public static OutreachAttemptResponse from(OutreachAttempt attempt) {
        return new OutreachAttemptResponse(
                attempt.getAttemptId(),
                attempt.getCandidateId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getTechnicalFitScore(),
                attempt.getTargetJobId(),
                attempt.getStatus(),
                attempt.getAttemptedAt()
        );
    }
}
