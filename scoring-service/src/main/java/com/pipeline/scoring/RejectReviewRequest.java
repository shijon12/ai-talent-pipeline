package com.pipeline.scoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectReviewRequest(
        @NotBlank @Size(max = 100) String reviewer,
        @NotBlank @Size(max = 2_000) String reason
) {
}
