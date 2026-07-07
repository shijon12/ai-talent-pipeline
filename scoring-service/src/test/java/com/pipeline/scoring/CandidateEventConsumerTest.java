package com.pipeline.scoring;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class CandidateEventConsumerTest {

    @Test
    void delegatesCandidateToTransactionalScoringService() {
        CandidateScoringService scoringService =
                org.mockito.Mockito.mock(CandidateScoringService.class);
        CandidateApplication candidate = new CandidateApplication(
                "candidate-001",
                "Test Candidate",
                "candidate@example.com",
                "Java and Kafka",
                "job-101"
        );

        new CandidateEventConsumer(scoringService)
                .scoreCandidate()
                .accept(candidate);

        verify(scoringService).scoreAndPersist(candidate);
    }
}
