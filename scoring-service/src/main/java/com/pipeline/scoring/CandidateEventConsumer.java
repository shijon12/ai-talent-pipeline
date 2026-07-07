package com.pipeline.scoring;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CandidateEventConsumer {

    private final CandidateScoringService scoringService;

    @Bean
    public Consumer<CandidateApplication> scoreCandidate() {
        return scoringService::scoreAndPersist;
    }
}
