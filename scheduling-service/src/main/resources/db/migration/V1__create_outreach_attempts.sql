CREATE TABLE outreach_attempts (
    attempt_id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(100) NOT NULL UNIQUE,
    candidate_name VARCHAR(200) NOT NULL,
    candidate_email VARCHAR(320) NOT NULL,
    technical_fit_score INTEGER NOT NULL,
    target_job_id VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_outreach_attempted_at
    ON outreach_attempts (attempted_at DESC);
