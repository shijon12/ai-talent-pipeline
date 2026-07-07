package com.pipeline.scoring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringOutboxEventRepository
        extends JpaRepository<ScoringOutboxEvent, String> {

    List<ScoringOutboxEvent>
            findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
