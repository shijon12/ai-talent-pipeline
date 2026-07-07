package com.pipeline.scoring;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CandidateScoreCalculator {

    private static final Set<String> RELEVANT_SKILLS = Set.of(
            "java",
            "spring",
            "kafka",
            "postgresql",
            "microservice"
    );

    public int calculate(CandidateApplication candidate) {
        int score = 10;

        if (candidate.email() != null && candidate.email().contains("@")) {
            score += 10;
        }
        if (hasText(candidate.targetJobId())) {
            score += 10;
        }

        String resume = candidate.resumeText() == null
                ? ""
                : candidate.resumeText().toLowerCase(Locale.ROOT);

        if (!resume.isBlank()) {
            score += Math.min(30, Math.max(1, resume.length() / 20));
        }

        for (String skill : RELEVANT_SKILLS) {
            if (resume.contains(skill)) {
                score += 8;
            }
        }

        return Math.min(100, score);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
