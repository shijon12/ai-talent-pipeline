package com.pipeline.scoring;

import java.time.Instant;

public record ReviewResponse(
        String candidateId,
        ReviewStatus status,
        String reviewer,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt,
        long version
) {
    public static ReviewResponse from(CandidateReview review) {
        return new ReviewResponse(
                review.getCandidateId(),
                review.getStatus(),
                review.getReviewer(),
                review.getReviewNote(),
                review.getCreatedAt(),
                review.getReviewedAt(),
                review.getVersion()
        );
    }
}
