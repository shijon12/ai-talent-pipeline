package com.pipeline.scoring;

import java.time.Instant;
import java.util.List;

public record JobResponse(
        String jobId,
        String title,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills,
        Instant createdAt,
        Instant updatedAt
) {
}
