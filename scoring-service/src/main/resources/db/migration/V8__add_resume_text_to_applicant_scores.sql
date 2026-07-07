ALTER TABLE applicant_scores
    ADD COLUMN IF NOT EXISTS resume_text TEXT;

