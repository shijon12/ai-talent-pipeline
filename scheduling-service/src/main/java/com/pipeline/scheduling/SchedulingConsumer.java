package com.pipeline.scheduling;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SchedulingConsumer {

    private final OutreachService outreachService;

    @Bean
    public Consumer<CandidateQualifiedEvent> processSchedule() {
        return event -> {
            outreachService.sendMockInvite(event);
        };
    }
}
