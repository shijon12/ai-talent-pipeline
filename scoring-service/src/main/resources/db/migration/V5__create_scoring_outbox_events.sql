CREATE TABLE scoring_outbox_events (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

CREATE INDEX idx_scoring_outbox_pending
    ON scoring_outbox_events (created_at)
    WHERE published_at IS NULL;
