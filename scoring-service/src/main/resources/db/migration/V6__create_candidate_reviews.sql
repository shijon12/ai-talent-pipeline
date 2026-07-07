CREATE TABLE candidate_reviews (
    candidate_id VARCHAR(100) PRIMARY KEY
        REFERENCES applicant_scores(candidate_id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    reviewer VARCHAR(100),
    review_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_candidate_reviews_status_created
    ON candidate_reviews (status, created_at);
