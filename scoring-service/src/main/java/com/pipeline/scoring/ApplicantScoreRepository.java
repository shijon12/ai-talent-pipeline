package com.pipeline.scoring;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicantScoreRepository extends JpaRepository<ApplicantScore, String> {

    List<ApplicantScore> findAllByOrderByScoredAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO applicant_scores (
                candidate_id,
                candidate_name,
                candidate_email,
                target_job_id,
                resume_text,
                score,
                scored_at,
                ai_analysis_status,
                ai_analysis_json
            )
            VALUES (
                :candidateId,
                :candidateName,
                :candidateEmail,
                :targetJobId,
                :resumeText,
                :score,
                :scoredAt,
                :aiAnalysisStatus,
                :aiAnalysisJson
            )
            ON CONFLICT (candidate_id) DO UPDATE SET
                candidate_name = EXCLUDED.candidate_name,
                candidate_email = EXCLUDED.candidate_email,
                target_job_id = EXCLUDED.target_job_id,
                resume_text = EXCLUDED.resume_text,
                score = EXCLUDED.score,
                scored_at = EXCLUDED.scored_at,
                ai_analysis_status = EXCLUDED.ai_analysis_status,
                ai_analysis_json = EXCLUDED.ai_analysis_json
            """, nativeQuery = true)
    void upsert(
            @Param("candidateId") String candidateId,
            @Param("candidateName") String candidateName,
            @Param("candidateEmail") String candidateEmail,
            @Param("targetJobId") String targetJobId,
            @Param("resumeText") String resumeText,
            @Param("score") int score,
            @Param("scoredAt") Instant scoredAt,
            @Param("aiAnalysisStatus") String aiAnalysisStatus,
            @Param("aiAnalysisJson") String aiAnalysisJson
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ApplicantScore score
            SET score.aiAnalysisStatus = :aiAnalysisStatus,
                score.aiAnalysisJson = :aiAnalysisJson,
                score.scoredAt = :scoredAt
            WHERE score.candidateId = :candidateId
            """)
    int updateAnalysis(
            @Param("candidateId") String candidateId,
            @Param("aiAnalysisStatus") String aiAnalysisStatus,
            @Param("aiAnalysisJson") String aiAnalysisJson,
            @Param("scoredAt") Instant scoredAt
    );
}
