package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CandidateReviewServiceTest {

    private CandidateReviewRepository reviewRepository;
    private ApplicantScoreRepository scoreRepository;
    private ScoringOutboxEventRepository outboxRepository;
    private CandidateReviewService service;

    @BeforeEach
    void setUp() {
        reviewRepository = org.mockito.Mockito.mock(
                CandidateReviewRepository.class
        );
        scoreRepository = org.mockito.Mockito.mock(
                ApplicantScoreRepository.class
        );
        outboxRepository = org.mockito.Mockito.mock(
                ScoringOutboxEventRepository.class
        );
        service = new CandidateReviewService(
                reviewRepository,
                scoreRepository,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void approvalAtomicallyStagesLifecycleAndQualifiedEvents() {
        CandidateReview review = new CandidateReview(
                "candidate-1",
                Instant.now()
        );
        ApplicantScore score = org.mockito.Mockito.mock(ApplicantScore.class);
        when(score.getCandidateName()).thenReturn("Alex Morgan");
        when(score.getCandidateEmail()).thenReturn("alex@example.com");
        when(score.getScore()).thenReturn(88);
        when(score.getTargetJobId()).thenReturn("job-101");
        when(reviewRepository.findForUpdate("candidate-1"))
                .thenReturn(Optional.of(review));
        when(scoreRepository.findById("candidate-1"))
                .thenReturn(Optional.of(score));

        ReviewResponse response = service.approve(
                "candidate-1",
                new ApproveReviewRequest("Reviewer One", "Evidence checked")
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(response.reviewer()).isEqualTo("Reviewer One");
        ArgumentCaptor<ScoringOutboxEvent> events =
                ArgumentCaptor.forClass(ScoringOutboxEvent.class);
        verify(outboxRepository, org.mockito.Mockito.times(2))
                .save(events.capture());
        assertThat(events.getAllValues())
                .extracting(ScoringOutboxEvent::getEventType)
                .containsExactly(
                        ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                        ScoringOutboxEventType.CANDIDATE_QUALIFIED
                );
        assertThat(events.getAllValues().get(0).getPayloadJson())
                .contains("APPROVED");
        assertThat(events.getAllValues().get(1).getPayloadJson())
                .contains("candidate-1", "Alex Morgan");
    }

    @Test
    void rejectionStagesOnlyRejectedLifecycleEvent() {
        CandidateReview review = new CandidateReview(
                "candidate-2",
                Instant.now()
        );
        when(reviewRepository.findForUpdate("candidate-2"))
                .thenReturn(Optional.of(review));

        ReviewResponse response = service.reject(
                "candidate-2",
                new RejectReviewRequest(
                        "Reviewer Two",
                        "Experience does not meet this role"
                )
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.REJECTED);
        ArgumentCaptor<ScoringOutboxEvent> event =
                ArgumentCaptor.forClass(ScoringOutboxEvent.class);
        verify(outboxRepository).save(event.capture());
        assertThat(event.getValue().getEventType())
                .isEqualTo(ScoringOutboxEventType.CANDIDATE_LIFECYCLE);
        assertThat(event.getValue().getPayloadJson()).contains("REJECTED");
        verify(scoreRepository, never()).findById(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void repeatedApprovalIsIdempotent() {
        CandidateReview review = new CandidateReview(
                "candidate-3",
                Instant.now()
        );
        review.approve("Reviewer One", null, Instant.now());
        when(reviewRepository.findForUpdate("candidate-3"))
                .thenReturn(Optional.of(review));

        ReviewResponse response = service.approve(
                "candidate-3",
                new ApproveReviewRequest("Reviewer One", null)
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.APPROVED);
        verify(scoreRepository, never()).findById(
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(outboxRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }
}
