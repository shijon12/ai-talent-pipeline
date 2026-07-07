package com.pipeline.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IngestionApplicationService {

    private static final String AGGREGATE_TYPE = "CandidateApplication";
    private static final String SOURCE = "ingestion-service";

    private final TransactionalOutboxRepository outboxRepository;
    private final CandidateLifecycleTrailRepository lifecycleRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationAcceptedResponse accept(
            String name,
            String email,
            String resumeText,
            String targetJobId
    ) {
        String candidateId = "candidate-" + UUID.randomUUID();
        Instant acceptedAt = Instant.now();
        CandidateApplication candidate = new CandidateApplication(
                candidateId,
                name.trim(),
                email.trim(),
                resumeText,
                targetJobId.trim()
        );
        CandidateLifecycleEvent lifecycle = new CandidateLifecycleEvent(
                deterministicId(candidateId, CandidateLifecycleStatus.INGESTED.name())
                        .toString(),
                candidateId,
                CandidateLifecycleStatus.INGESTED,
                acceptedAt,
                SOURCE
        );

        lifecycleRepository.save(new CandidateLifecycleTrail(lifecycle));
        outboxRepository.save(new TransactionalOutbox(
                deterministicId(
                        candidateId,
                        IngestionOutboxEventType.APPLICATION_INGESTED.name()
                ),
                AGGREGATE_TYPE,
                candidateId,
                IngestionOutboxEventType.APPLICATION_INGESTED,
                serialize(candidate),
                acceptedAt
        ));
        outboxRepository.save(new TransactionalOutbox(
                deterministicId(
                        candidateId,
                        IngestionOutboxEventType.CANDIDATE_LIFECYCLE.name()
                ),
                AGGREGATE_TYPE,
                candidateId,
                IngestionOutboxEventType.CANDIDATE_LIFECYCLE,
                serialize(lifecycle),
                acceptedAt.plusMillis(1)
        ));

        return new ApplicationAcceptedResponse(
                candidateId,
                CandidateLifecycleStatus.INGESTED
        );
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize ingestion outbox payload",
                    exception
            );
        }
    }

    private UUID deterministicId(String candidateId, String eventType) {
        return UUID.nameUUIDFromBytes(
                (candidateId + ":" + eventType)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
