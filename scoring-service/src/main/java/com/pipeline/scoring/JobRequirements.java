package com.pipeline.scoring;

import java.util.List;

public record JobRequirements(
        String jobId,
        String title,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills
) {
}
