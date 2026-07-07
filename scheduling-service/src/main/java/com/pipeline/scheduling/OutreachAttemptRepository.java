package com.pipeline.scheduling;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutreachAttemptRepository
        extends JpaRepository<OutreachAttempt, String> {

    Optional<OutreachAttempt> findByCandidateId(String candidateId);

    List<OutreachAttempt> findAllByOrderByAttemptedAtDesc();
}
