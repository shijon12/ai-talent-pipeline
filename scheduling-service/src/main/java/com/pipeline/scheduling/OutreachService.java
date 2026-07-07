package com.pipeline.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutreachService {

    private final OutreachAttemptRepository repository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutreachAttempt sendMockInvite(CandidateQualifiedEvent event) {
        return repository.findByCandidateId(event.candidateId())
                .orElseGet(() -> createAttemptAndOutboxEvent(event));
    }

    private OutreachAttempt createAttemptAndOutboxEvent(
            CandidateQualifiedEvent event
    ) {
        log.info(
                "[AGENT] Candidate {} ({}) qualified with score {}! "
                        + "Generating autonomous calendar invite via mock outreach API...",
                event.name(),
                event.email(),
                event.technicalFitScore()
        );
        String attemptId = UUID.nameUUIDFromBytes(
                (event.candidateId() + ":OUTREACH")
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
        OutreachAttempt attempt = repository.save(
                new OutreachAttempt(attemptId, event, Instant.now())
        );
        CandidateLifecycleEvent lifecycleEvent = lifecycleEvent(event);
        outboxRepository.save(
                new OutboxEvent(
                        lifecycleEvent,
                        serialize(lifecycleEvent)
                )
        );
        return attempt;
    }

    private CandidateLifecycleEvent lifecycleEvent(
            CandidateQualifiedEvent event
    ) {
        CandidateLifecycleStatus status =
                CandidateLifecycleStatus.OUTREACH_SENT;
        return new CandidateLifecycleEvent(
                UUID.nameUUIDFromBytes(
                        (event.candidateId() + ":" + status.name())
                                .getBytes(StandardCharsets.UTF_8)
                ).toString(),
                event.candidateId(),
                status,
                Instant.now(),
                "scheduling-service"
        );
    }

    private String serialize(CandidateLifecycleEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize lifecycle outbox event",
                    exception
            );
        }
    }
}
