package com.pipeline.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outreach_attempts", schema = "scheduling")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutreachAttempt {

    @Id
    @Column(name = "attempt_id", nullable = false, length = 36)
    private String attemptId;

    @Column(name = "candidate_id", nullable = false, unique = true, length = 100)
    private String candidateId;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 320)
    private String candidateEmail;

    @Column(name = "technical_fit_score", nullable = false)
    private int technicalFitScore;

    @Column(name = "target_job_id", nullable = false, length = 100)
    private String targetJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutreachAttemptStatus status;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    public OutreachAttempt(
            String attemptId,
            CandidateQualifiedEvent event,
            Instant attemptedAt
    ) {
        this.attemptId = attemptId;
        this.candidateId = event.candidateId();
        this.candidateName = event.name();
        this.candidateEmail = event.email();
        this.technicalFitScore = event.technicalFitScore();
        this.targetJobId = event.targetJobId();
        this.status = OutreachAttemptStatus.SENT;
        this.attemptedAt = attemptedAt;
    }
}
