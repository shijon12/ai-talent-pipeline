package com.pipeline.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applicant_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicantScore {

    @Id
    @Column(name = "candidate_id", nullable = false, length = 100)
    private String candidateId;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 320)
    private String candidateEmail;

    @Column(name = "target_job_id", nullable = false, length = 100)
    private String targetJobId;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(nullable = false)
    private int score;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @Column(name = "ai_analysis_status", nullable = false, length = 32)
    private String aiAnalysisStatus;

    @Column(name = "ai_analysis_json", columnDefinition = "TEXT")
    private String aiAnalysisJson;
}
