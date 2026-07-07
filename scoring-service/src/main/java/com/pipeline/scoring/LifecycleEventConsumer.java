package com.pipeline.scoring;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class LifecycleEventConsumer {

    private final CandidateLifecycleEventRepository repository;

    @Bean
    public Consumer<CandidateLifecycleEvent> trackLifecycle() {
        return event -> {
            validate(event);
            repository.save(new CandidateLifecycleEventEntity(event));
        };
    }

    private void validate(CandidateLifecycleEvent event) {
        if (event == null
                || isBlank(event.eventId())
                || isBlank(event.candidateId())
                || event.status() == null
                || event.occurredAt() == null
                || isBlank(event.source())) {
            throw new IllegalArgumentException(
                    "Lifecycle event is incomplete"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
