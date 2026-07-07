package com.pipeline.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CandidateReviewService {

    private final CandidateReviewRepository reviewRepository;
    private final ApplicantScoreRepository scoreRepository;
    private final ScoringOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ReviewResponse> findReviews(ReviewStatus status) {
        List<CandidateReview> reviews = status == null
                ? reviewRepository.findAllByOrderByCreatedAtDesc()
                : reviewRepository.findByStatusOrderByCreatedAtDesc(status);
        return reviews.stream().map(ReviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse findReview(String candidateId) {
        return ReviewResponse.from(find(candidateId));
    }

    @Transactional
    public ReviewResponse approve(
            String candidateId,
            ApproveReviewRequest request
    ) {
        CandidateReview review = findForUpdate(candidateId);
        if (review.getStatus() == ReviewStatus.APPROVED) {
            return ReviewResponse.from(review);
        }
        requirePending(review, "approve");

        ApplicantScore score = scoreRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate score not found"
                ));
        Instant approvedAt = Instant.now();
        review.approve(
                request.reviewer().trim(),
                request.note(),
                approvedAt
        );

        CandidateLifecycleEvent lifecycle = lifecycleEvent(
                candidateId,
                CandidateLifecycleStatus.APPROVED,
                approvedAt
        );
        saveOutbox(
                lifecycle.eventId(),
                candidateId,
                ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                lifecycle,
                approvedAt
        );

        CandidateQualifiedEvent qualified = new CandidateQualifiedEvent(
                candidateId,
                score.getCandidateName(),
                score.getCandidateEmail(),
                score.getScore(),
                score.getTargetJobId()
        );
        saveOutbox(
                deterministicId(candidateId, "CANDIDATE_QUALIFIED"),
                candidateId,
                ScoringOutboxEventType.CANDIDATE_QUALIFIED,
                qualified,
                approvedAt.plusMillis(1)
        );

        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse reject(
            String candidateId,
            RejectReviewRequest request
    ) {
        CandidateReview review = findForUpdate(candidateId);
        if (review.getStatus() == ReviewStatus.REJECTED) {
            return ReviewResponse.from(review);
        }
        requirePending(review, "reject");

        Instant rejectedAt = Instant.now();
        review.reject(
                request.reviewer().trim(),
                request.reason(),
                rejectedAt
        );
        CandidateLifecycleEvent lifecycle = lifecycleEvent(
                candidateId,
                CandidateLifecycleStatus.REJECTED,
                rejectedAt
        );
        saveOutbox(
                lifecycle.eventId(),
                candidateId,
                ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                lifecycle,
                rejectedAt
        );
        return ReviewResponse.from(review);
    }

    private CandidateReview find(String candidateId) {
        return reviewRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate review not found"
                ));
    }

    private CandidateReview findForUpdate(String candidateId) {
        return reviewRepository.findForUpdate(candidateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate review not found"
                ));
    }

    private void requirePending(CandidateReview review, String action) {
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot %s a %s review".formatted(
                            action,
                            review.getStatus().name().toLowerCase()
                    )
            );
        }
    }

    private CandidateLifecycleEvent lifecycleEvent(
            String candidateId,
            CandidateLifecycleStatus status,
            Instant occurredAt
    ) {
        return new CandidateLifecycleEvent(
                deterministicId(candidateId, status.name()),
                candidateId,
                status,
                occurredAt,
                "scoring-service"
        );
    }

    private void saveOutbox(
            String eventId,
            String candidateId,
            ScoringOutboxEventType eventType,
            Object payload,
            Instant createdAt
    ) {
        outboxRepository.save(new ScoringOutboxEvent(
                eventId,
                candidateId,
                eventType,
                serialize(payload),
                createdAt
        ));
    }

    private String deterministicId(String candidateId, String eventType) {
        return UUID.nameUUIDFromBytes(
                (candidateId + ":" + eventType)
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize review outbox payload",
                    exception
            );
        }
    }
}
