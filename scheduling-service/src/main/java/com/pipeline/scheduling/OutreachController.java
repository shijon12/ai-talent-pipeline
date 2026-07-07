package com.pipeline.scheduling;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/outreach")
@RequiredArgsConstructor
public class OutreachController {

    private final OutreachAttemptRepository repository;

    @GetMapping
    public List<OutreachAttemptResponse> findAll() {
        return repository.findAllByOrderByAttemptedAtDesc().stream()
                .map(OutreachAttemptResponse::from)
                .toList();
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<OutreachAttemptResponse> findByCandidate(
            @PathVariable String candidateId
    ) {
        return repository.findByCandidateId(candidateId)
                .map(OutreachAttemptResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
