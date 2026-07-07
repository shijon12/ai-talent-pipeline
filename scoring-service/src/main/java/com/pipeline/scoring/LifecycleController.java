package com.pipeline.scoring;

import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/lifecycle")
@RequiredArgsConstructor
public class LifecycleController {

    private final CandidateLifecycleEventRepository repository;

    @GetMapping("/latest")
    public List<LifecycleEventResponse> latest() {
        LinkedHashMap<String, LifecycleEventResponse> latestByCandidate =
                new LinkedHashMap<>();
        repository.findAllByOrderByOccurredAtDesc().stream()
                .map(LifecycleEventResponse::from)
                .forEach(event -> latestByCandidate.putIfAbsent(
                        event.candidateId(),
                        event
                ));
        return List.copyOf(latestByCandidate.values());
    }

    @GetMapping("/{candidateId}")
    public List<LifecycleEventResponse> timeline(
            @PathVariable String candidateId
    ) {
        return repository
                .findByCandidateIdOrderByOccurredAtAsc(candidateId)
                .stream()
                .map(LifecycleEventResponse::from)
                .toList();
    }
}
