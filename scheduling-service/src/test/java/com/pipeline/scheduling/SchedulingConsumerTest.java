package com.pipeline.scheduling;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulingConsumerTest {

    private OutreachService outreachService;
    private SchedulingConsumer consumer;

    @BeforeEach
    void setUp() {
        outreachService = org.mockito.Mockito.mock(OutreachService.class);
        consumer = new SchedulingConsumer(outreachService);
    }

    @Test
    void delegatesQualifiedCandidateToTransactionalOutreachService() {
        CandidateQualifiedEvent event = new CandidateQualifiedEvent(
                "candidate-008",
                "Morgan Chen",
                "morgan@example.com",
                77,
                "job-101"
        );

        consumer.processSchedule().accept(event);

        verify(outreachService).sendMockInvite(event);
    }
}
