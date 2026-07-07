package com.pipeline.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;

public record ScoreResponse(
        String candidateId,
        String name,
        String email,
        String targetJobId,
        int score,
        Integer experienceAlignmentScore,
        Instant scoredAt,
        CandidateAnalysis advisoryAnalysis,
        String reviewPolicy,
        boolean aiRetryAvailable
) {
    public static ScoreResponse from(
            ApplicantScore applicantScore,
            ObjectMapper objectMapper
    ) {
        CandidateAnalysis analysis = deserializeAnalysis(applicantScore, objectMapper);
        return new ScoreResponse(
                applicantScore.getCandidateId(),
                applicantScore.getCandidateName(),
                applicantScore.getCandidateEmail(),
                applicantScore.getTargetJobId(),
                applicantScore.getScore(),
                calculateExperienceAlignment(analysis),
                applicantScore.getScoredAt(),
                analysis,
                "HUMAN_REVIEW_REQUIRED",
                applicantScore.getResumeText() != null
                        && !applicantScore.getResumeText().isBlank()
        );
    }

    private static Integer calculateExperienceAlignment(CandidateAnalysis analysis) {
        if (analysis.status() != CandidateAnalysis.Status.COMPLETED) {
            return null;
        }
        int matched = analysis.matchedRequiredSkills().size();
        int total = matched + analysis.requiredSkillsNotEvidenced().size();
        return total == 0 ? null : Math.round((matched * 100.0f) / total);
    }

    private static CandidateAnalysis deserializeAnalysis(
            ApplicantScore applicantScore,
            ObjectMapper objectMapper
    ) {
        if (applicantScore.getAiAnalysisJson() == null
                || applicantScore.getAiAnalysisJson().isBlank()) {
            return CandidateAnalysis.notAnalyzed();
        }
        try {
            return objectMapper.readValue(
                    applicantScore.getAiAnalysisJson(),
                    CandidateAnalysis.class
            );
        }
        catch (JsonProcessingException exception) {
            return CandidateAnalysis.unavailable();
        }
    }
}
