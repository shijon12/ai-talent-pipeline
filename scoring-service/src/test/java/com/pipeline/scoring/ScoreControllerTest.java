package com.pipeline.scoring;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScoreControllerTest {

    @Test
    void returnsScoresAsAJsonCollectionNewestFirst() throws Exception {
        ApplicantScoreRepository repository =
                org.mockito.Mockito.mock(ApplicantScoreRepository.class);
        CandidateScoringService scoringService =
                org.mockito.Mockito.mock(CandidateScoringService.class);
        when(repository.findAllByOrderByScoredAtDesc()).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ScoreController(
                        repository,
                        new ObjectMapper(),
                        scoringService
                ))
                .build();

        mockMvc.perform(get("/v1/scores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));

        verify(repository).findAllByOrderByScoredAtDesc();
    }
}
