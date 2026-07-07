CREATE TABLE outbox_events (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
