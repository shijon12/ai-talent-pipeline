package com.pipeline.scoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveReviewRequest(
        @NotBlank @Size(max = 100) String reviewer,
        @Size(max = 2_000) String note
) {
}
