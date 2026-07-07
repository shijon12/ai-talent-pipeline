package com.pipeline.scoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobRequest(
        @NotBlank @Size(max = 100) String jobId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10_000) String description,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 100) String> requiredSkills,
        @Size(max = 50) List<@NotBlank @Size(max = 100) String> preferredSkills
) {
}
