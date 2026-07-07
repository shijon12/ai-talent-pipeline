ALTER TABLE applicant_scores
    ADD COLUMN ai_analysis_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ANALYZED',
    ADD COLUMN ai_analysis_json TEXT;
