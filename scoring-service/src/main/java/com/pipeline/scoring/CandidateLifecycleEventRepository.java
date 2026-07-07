package com.pipeline.scoring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateLifecycleEventRepository
        extends JpaRepository<CandidateLifecycleEventEntity, String> {

    List<CandidateLifecycleEventEntity>
            findByCandidateIdOrderByOccurredAtAsc(String candidateId);

    List<CandidateLifecycleEventEntity> findAllByOrderByOccurredAtDesc();
}
