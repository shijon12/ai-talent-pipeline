package com.pipeline.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ApplicantScoreRepository scoreRepository;
    private final ObjectMapper objectMapper;
    private final CandidateScoringService scoringService;

    @GetMapping
    public List<ScoreResponse> findScores() {
        return scoreRepository.findAllByOrderByScoredAtDesc().stream()
                .map(score -> ScoreResponse.from(score, objectMapper))
                .toList();
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<ScoreResponse> findScore(
            @PathVariable String candidateId
    ) {
        return scoreRepository.findById(candidateId)
                .map(score -> ScoreResponse.from(score, objectMapper))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{candidateId}/retry-ai")
    public ResponseEntity<?> retryAiAnalysis(@PathVariable String candidateId) {
        try {
            scoringService.retryAiAnalysis(candidateId);
            return scoreRepository.findById(candidateId)
                    .map(score -> ScoreResponse.from(score, objectMapper))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        catch (AiRetryUnavailableException exception) {
            return ResponseEntity.status(409)
                    .body(new RetryAiErrorResponse(exception.getMessage()));
        }
    }
}
