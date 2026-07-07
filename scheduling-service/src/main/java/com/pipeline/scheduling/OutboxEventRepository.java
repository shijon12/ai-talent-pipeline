package com.pipeline.scheduling;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent>
            findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
