package com.pipeline.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidate_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CandidateReview {

    @Id
    @Column(name = "candidate_id", nullable = false, length = 100)
    private String candidateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewStatus status;

    @Column(length = 100)
    private String reviewer;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public CandidateReview(String candidateId, Instant createdAt) {
        this.candidateId = candidateId;
        this.status = ReviewStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void approve(String reviewer, String note, Instant reviewedAt) {
        this.status = ReviewStatus.APPROVED;
        this.reviewer = reviewer;
        this.reviewNote = normalize(note);
        this.reviewedAt = reviewedAt;
        this.updatedAt = reviewedAt;
    }

    public void reject(String reviewer, String note, Instant reviewedAt) {
        this.status = ReviewStatus.REJECTED;
        this.reviewer = reviewer;
        this.reviewNote = normalize(note);
        this.reviewedAt = reviewedAt;
        this.updatedAt = reviewedAt;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
