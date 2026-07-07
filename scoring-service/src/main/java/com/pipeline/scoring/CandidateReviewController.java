package com.pipeline.scoring;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class CandidateReviewController {

    private final CandidateReviewService reviewService;

    @GetMapping
    public List<ReviewResponse> findReviews(
            @RequestParam(required = false) ReviewStatus status
    ) {
        return reviewService.findReviews(status);
    }

    @GetMapping("/{candidateId}")
    public ReviewResponse findReview(@PathVariable String candidateId) {
        return reviewService.findReview(candidateId);
    }

    @PostMapping("/{candidateId}/approve")
    public ResponseEntity<ReviewResponse> approve(
            @PathVariable String candidateId,
            @Valid @RequestBody ApproveReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.approve(candidateId, request));
    }

    @PostMapping("/{candidateId}/reject")
    public ResponseEntity<ReviewResponse> reject(
            @PathVariable String candidateId,
            @Valid @RequestBody RejectReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.reject(candidateId, request));
    }
}
