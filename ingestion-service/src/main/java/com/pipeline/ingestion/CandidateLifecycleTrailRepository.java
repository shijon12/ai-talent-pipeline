package com.pipeline.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateLifecycleTrailRepository
        extends JpaRepository<CandidateLifecycleTrail, String> {
}
