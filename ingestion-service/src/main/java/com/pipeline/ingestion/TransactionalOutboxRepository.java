package com.pipeline.ingestion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionalOutboxRepository
        extends JpaRepository<TransactionalOutbox, UUID> {

    @Query(
            value = """
                    SELECT *
                    FROM ingestion.transactional_outbox
                    WHERE status = 'PENDING'
                    ORDER BY created_at
                    LIMIT 10
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<TransactionalOutbox> lockOldestPending();
}
