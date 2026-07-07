package com.pipeline.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateScoringService {

    static final int QUALIFICATION_THRESHOLD = 75;

    private final CandidateScoreCalculator scoreCalculator;
    private final CandidateAnalysisService analysisService;
    private final ApplicantScoreRepository scoreRepository;
    private final CandidateReviewRepository reviewRepository;
    private final ScoringOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int scoreAndPersist(CandidateApplication candidate) {
        validate(candidate);

        int score = scoreCalculator.calculate(candidate);
        CandidateAnalysis analysis = analysisService.analyze(candidate);
        Instant scoredAt = Instant.now();
        scoreRepository.upsert(
                candidate.id(),
                candidate.name(),
                candidate.email(),
                candidate.targetJobId(),
                candidate.resumeText(),
                score,
                scoredAt,
                analysis.status().name(),
                serialize(analysis)
        );
        reviewRepository.findById(candidate.id()).orElseGet(
                () -> reviewRepository.save(
                        new CandidateReview(candidate.id(), scoredAt)
                )
        );
        enqueueReviewEvents(candidate, scoredAt);

        log.info(
                "Scored candidateId={} for targetJobId={} with score={} advisoryStatus={}",
                candidate.id(),
                candidate.targetJobId(),
                score,
                analysis.status()
        );
        return score;
    }

    @Transactional
    public CandidateAnalysis retryAiAnalysis(String candidateId) {
        ApplicantScore existing = scoreRepository.findById(candidateId)
                .orElseThrow(() -> new AiRetryUnavailableException(
                        "Candidate score was not found."
                ));
        if (existing.getResumeText() == null || existing.getResumeText().isBlank()) {
            throw new AiRetryUnavailableException(
                    "This candidate was scored before resume snapshots were stored. "
                            + "Upload the resume again to enable AI retry."
            );
        }

        CandidateApplication candidate = new CandidateApplication(
                existing.getCandidateId(),
                existing.getCandidateName(),
                existing.getCandidateEmail(),
                existing.getResumeText(),
                existing.getTargetJobId()
        );
        CandidateAnalysis analysis = analysisService.analyze(candidate);
        scoreRepository.updateAnalysis(
                candidate.id(),
                analysis.status().name(),
                serialize(analysis),
                Instant.now()
        );
        log.info(
                "Retried AI analysis for candidateId={} with advisoryStatus={}",
                candidate.id(),
                analysis.status()
        );
        return analysis;
    }

    private void enqueueReviewEvents(
            CandidateApplication candidate,
            Instant scoredAt
    ) {
        CandidateLifecycleEvent scoredEvent = lifecycleEvent(
                candidate.id(),
                CandidateLifecycleStatus.SCORED,
                scoredAt
        );
        saveOutbox(
                scoredEvent.eventId(),
                candidate.id(),
                ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                scoredEvent,
                scoredAt
        );

        CandidateLifecycleEvent reviewRequired = lifecycleEvent(
                candidate.id(),
                CandidateLifecycleStatus.REVIEW_REQUIRED,
                scoredAt.plusMillis(1)
        );
        saveOutbox(
                reviewRequired.eventId(),
                candidate.id(),
                ScoringOutboxEventType.CANDIDATE_LIFECYCLE,
                reviewRequired,
                scoredAt.plusMillis(1)
        );
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
        outboxRepository.save(
                new ScoringOutboxEvent(
                        eventId,
                        candidateId,
                        eventType,
                        serialize(payload),
                        createdAt
                )
        );
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
                    "Failed to serialize scoring payload",
                    exception
            );
        }
    }

    private void validate(CandidateApplication candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate event must not be null");
        }
        requireText(candidate.id(), "id");
        requireText(candidate.name(), "name");
        requireText(candidate.email(), "email");
        requireText(candidate.targetJobId(), "targetJobId");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Candidate event field '%s' must not be blank".formatted(fieldName)
            );
        }
    }
}
