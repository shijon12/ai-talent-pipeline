package com.pipeline.scoring;

import java.util.List;

public record ModelCandidateAnalysis(
        List<String> identifiedSkills,
        List<String> matchedRequiredSkills,
        List<String> matchedPreferredSkills,
        List<String> evidence,
        String summary
) {
}
