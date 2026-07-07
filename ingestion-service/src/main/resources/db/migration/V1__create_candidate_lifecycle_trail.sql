CREATE TABLE candidate_lifecycle_trail (
    event_id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(64) NOT NULL
);

CREATE INDEX idx_ingestion_lifecycle_candidate_time
    ON candidate_lifecycle_trail (candidate_id, occurred_at);
