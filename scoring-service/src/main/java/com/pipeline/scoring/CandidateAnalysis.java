package com.pipeline.scoring;

import java.util.List;

public record CandidateAnalysis(
        Status status,
        List<String> identifiedSkills,
        List<String> matchedRequiredSkills,
        List<String> requiredSkillsNotEvidenced,
        List<String> matchedPreferredSkills,
        List<String> evidence,
        String summary,
        String model,
        String promptVersion
) {
    public enum Status {
        COMPLETED,
        UNAVAILABLE,
        JOB_NOT_FOUND,
        NOT_ANALYZED
    }

    public CandidateAnalysis {
        identifiedSkills = identifiedSkills == null
                ? List.of()
                : List.copyOf(identifiedSkills);
        matchedRequiredSkills = copy(matchedRequiredSkills);
        requiredSkillsNotEvidenced = copy(requiredSkillsNotEvidenced);
        matchedPreferredSkills = copy(matchedPreferredSkills);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        summary = summary == null ? "" : summary;
        model = model == null ? "" : model;
        promptVersion = promptVersion == null ? "" : promptVersion;
    }

    public static CandidateAnalysis completed(
            List<String> identifiedSkills,
            List<String> matchedRequiredSkills,
            List<String> requiredSkillsNotEvidenced,
            List<String> matchedPreferredSkills,
            List<String> evidence,
            String summary,
            String model,
            String promptVersion
    ) {
        return new CandidateAnalysis(
                Status.COMPLETED,
                identifiedSkills,
                matchedRequiredSkills,
                requiredSkillsNotEvidenced,
                matchedPreferredSkills,
                evidence,
                summary,
                model,
                promptVersion
        );
    }

    public static CandidateAnalysis unavailable() {
        return unavailable(
                "Local AI analysis was unavailable; deterministic scoring was used."
        );
    }

    public static CandidateAnalysis unavailable(String summary) {
        return new CandidateAnalysis(
                Status.UNAVAILABLE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                summary,
                "llama3.2:1b",
                "job-match-v1"
        );
    }

    public static CandidateAnalysis jobNotFound() {
        return new CandidateAnalysis(
                Status.JOB_NOT_FOUND,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "No stored job requirements were found; advisory matching was skipped.",
                "",
                "job-match-v1"
        );
    }

    public static CandidateAnalysis notAnalyzed() {
        return new CandidateAnalysis(
                Status.NOT_ANALYZED,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "This score predates advisory analysis.",
                "",
                ""
        );
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
