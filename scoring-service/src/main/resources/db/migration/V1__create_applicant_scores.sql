CREATE TABLE applicant_scores (
    candidate_id VARCHAR(100) PRIMARY KEY,
    candidate_name VARCHAR(200) NOT NULL,
    candidate_email VARCHAR(320) NOT NULL,
    target_job_id VARCHAR(100) NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    scored_at TIMESTAMP WITH TIME ZONE NOT NULL
);
